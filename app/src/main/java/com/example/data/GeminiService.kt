package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.PricingType
import com.example.model.ProjectAnalysis
import com.example.model.SuggestedTool
import com.example.model.ToolCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeProject(
        prompt: String,
        documentContent: String? = null,
        forceFreshMarketSearch: Boolean = false
    ): ProjectAnalysis = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isKeyValid = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                apiKey != "null" &&
                !apiKey.contains("PLACEHOLDER", ignoreCase = true)

        val fullProjectText = buildString {
            append("User Project Prompt: ").append(prompt.trim())
            if (!documentContent.isNullOrBlank()) {
                append("\n\nAttached Project Specification Document:\n")
                append(documentContent.trim().take(6000))
            }
        }

        if (isKeyValid) {
            try {
                val result = callGeminiApi(apiKey, fullProjectText, forceFreshMarketSearch)
                if (result != null && result.tools.isNotEmpty()) {
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Gemini API call failed, falling back to smart synthesizer", e)
            }
        }

        // High quality offline / smart synthesizer fallback
        synthesizeSmartAnalysis(prompt, documentContent, forceFreshMarketSearch)
    }

    private fun callGeminiApi(
        apiKey: String,
        projectText: String,
        forceFreshMarketSearch: Boolean
    ): ProjectAnalysis? {
        val systemPrompt = """
            You are TOOLSUGGESTOR, an expert AI software architect and tech tool recommender for college students.
            The user is a college student building a major/minor or capstone project.
            They need an EXHAUSTIVE, ENCYCLOPEDIC tool recommendation covering the complete A to Z of tools available on the internet and market up to 2026 based on their project prompt and document.
            
            They need to know EXACTLY which tools to use across every category:
            1. Coding & AI Assist - Free tools (Cursor Free, GitHub Copilot, Windsurf, Continue.dev, Insomnia, Playwright, Zod) AND High-Performance Elite Paid Code Generators (Ox Alpha [0xAlpha], Devin AI, Cursor Pro, Claude Code CLI, Augment Code, Bolt.new Pro, Replit Agent, Qodo Gen, Tabnine Pro).
            2. Frontend - (e.g. Next.js 19, v0, Flutter, React Native/Expo, Jetpack Compose, Tailwind CSS v4, Shadcn, Astro 5, SvelteKit, Vite 6, Yjs, Zustand, Lovable.dev).
            3. Backend & APIs - (e.g. Supabase, Firebase, FastAPI, Node.js/Express, Bun, Better Auth, Clerk, Convex, Hono.js, Ktor, Resend, Stripe, Socket.IO, Uvicorn, PocketBase).
            4. Database & Storage - (e.g. Neon Serverless Postgres, MongoDB Atlas, Cloudflare D1, InfluxDB, Meilisearch, Pinecone Vector, Prisma ORM, Qdrant, Upstash Redis, Weaviate, Xata).
            5. Deploy & Cloud - (e.g. Vercel, Render, Railway, Fly.io, Docker & Compose, Kubernetes, Zeabur, AWS).
            6. AI Models & LLMs - (e.g. Gemini 3.5 Flash, DeepSeek-R1 & V3, GPT-6 Astra & GPT-4o, Claude 3.7 Sonnet, Groq LPU, Ollama Local, Hugging Face, Jina AI, Llama 3.3, Mistral/Codestral, ElevenLabs, XGBoost, YOLOv11).
            
            CRITICAL REQUIREMENTS:
            - Incorporate the entire modern landscape of 2025/2026 tech tools available on the internet and market.
            - Ensure tools spanning the letters of the alphabet from A to Z are recommended so the student has an encyclopedic directory.
            - For each tool, clearly classify:
              * pricing: either "UNPAID" (has a free tier, 100% free open source, or free student pack) or "PAID" (requires subscription or money to use).
              * category: one of "CODE", "FRONTEND", "BACKEND", "DATABASE", "DEPLOY", "AI_MODELS".
              * whyFitsProject: 1-2 direct sentences explaining specifically why this tool is ideal for THIS student's project idea.
              * studentTip: college advice like student discounts, free allowances, or submission grading advice.
              * starterCommand: sample terminal command or setup snippet.
            - Provide an extensive, comprehensive recommendation list covering both FREE/UNPAID and PAID tools.
            
            Output MUST be valid JSON conforming strictly to this format:
            {
              "projectTitle": "Short descriptive title for this project",
              "projectOverview": "2-3 sentence technical overview and architecture recommendation for this student project",
              "recommendedArchitecture": "Frontend: [X] | Backend: [Y] | DB: [Z] | AI: [W]",
              "studentCostSummary": "Estimated student cost: ${'$'}0 using free tiers (or ${'$'}X/mo if using paid options)",
              "tools": [
                {
                  "name": "Tool Name",
                  "tagline": "Short punchy description",
                  "category": "CODE | FRONTEND | BACKEND | DATABASE | DEPLOY | AI_MODELS",
                  "pricing": "UNPAID | PAID",
                  "pricingDetails": "Details about free limits or subscription cost",
                  "whyFitsProject": "Why it specifically fits this student project",
                  "keyFeatures": ["Feature 1", "Feature 2", "Feature 3"],
                  "studentTip": "Helpful tip for college students",
                  "isNewOrTrending": true,
                  "releaseBadge": "e.g. Latest 2025/2026 Release or Free with Student Pack",
                  "starterGuide": "Step 1: Install... Step 2: Configure...",
                  "starterCommand": "npm install ...",
                  "officialUrl": "https://..."
                }
              ]
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Please analyze this student project and provide the complete toolchain recommendations including the latest market releases:\n\n$projectText")
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("responseMimeType", "application/json")
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            Log.w("GeminiService", "Gemini HTTP error ${response.code}: ${response.message}")
            return null
        }

        val responseString = response.body?.string() ?: return null
        val rootJson = JSONObject(responseString)
        val candidates = rootJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val text = parts.getJSONObject(0).optString("text")
        if (text.isNullOrBlank()) return null

        return parseJsonResponse(text)
    }

    private fun parseJsonResponse(rawJson: String): ProjectAnalysis? {
        return try {
            val cleanJson = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleanJson)
            val projectTitle = json.optString("projectTitle", "College Project Architecture")
            val projectOverview = json.optString("projectOverview", "Recommended technical architecture and toolchain.")
            val recommendedArchitecture = json.optString("recommendedArchitecture", "Modern Fullstack Stack")
            val studentCostSummary = json.optString("studentCostSummary", "100% Free with student tiers")

            val toolsArray = json.optJSONArray("tools") ?: JSONArray()
            val toolsList = mutableListOf<SuggestedTool>()

            for (i in 0 until toolsArray.length()) {
                val toolObj = toolsArray.getJSONObject(i)
                val catStr = toolObj.optString("category", "CODE")
                val category = when (catStr.uppercase()) {
                    "FRONTEND" -> ToolCategory.FRONTEND
                    "BACKEND" -> ToolCategory.BACKEND
                    "DATABASE" -> ToolCategory.DATABASE
                    "DEPLOY" -> ToolCategory.DEPLOY
                    "AI_MODELS" -> ToolCategory.AI_MODELS
                    else -> ToolCategory.CODE
                }

                val priceStr = toolObj.optString("pricing", "UNPAID")
                val pricing = if (priceStr.contains("PAID", ignoreCase = true)) PricingType.PAID else PricingType.UNPAID

                val featuresArray = toolObj.optJSONArray("keyFeatures")
                val features = mutableListOf<String>()
                if (featuresArray != null) {
                    for (j in 0 until featuresArray.length()) {
                        features.add(featuresArray.getString(j))
                    }
                }

                toolsList.add(
                    SuggestedTool(
                        id = "gemini_${i}_${toolObj.optString("name").replace("\\s+".toRegex(), "_").lowercase()}",
                        name = toolObj.optString("name"),
                        tagline = toolObj.optString("tagline"),
                        category = category,
                        pricing = pricing,
                        pricingDetails = toolObj.optString("pricingDetails", if (pricing == PricingType.UNPAID) "Free Tier" else "Paid Subscription"),
                        whyFitsProject = toolObj.optString("whyFitsProject"),
                        keyFeatures = if (features.isEmpty()) listOf("Modern developer experience", "Cloud scale", "Fast setup") else features,
                        studentTip = toolObj.optString("studentTip", "Check for student developer discounts with your .edu email."),
                        isNewOrTrending = toolObj.optBoolean("isNewOrTrending", true),
                        releaseBadge = toolObj.optString("releaseBadge", if (pricing == PricingType.UNPAID) "Free Student Choice" else "Pro Tool"),
                        starterGuide = toolObj.optString("starterGuide", "Visit the official documentation to initialize your project."),
                        starterCommand = toolObj.optString("starterCommand", ""),
                        officialUrl = toolObj.optString("officialUrl", "")
                    )
                )
            }

            ProjectAnalysis(
                projectTitle = projectTitle,
                projectOverview = projectOverview,
                recommendedArchitecture = recommendedArchitecture,
                studentCostSummary = studentCostSummary,
                tools = toolsList
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Error parsing Gemini JSON response", e)
            null
        }
    }

    fun synthesizeSmartAnalysis(
        prompt: String,
        documentContent: String?,
        includeFreshMarketRadar: Boolean = false
    ): ProjectAnalysis {
        val combinedText = "$prompt ${documentContent ?: ""}".lowercase()

        val isMobile = combinedText.contains("android") || combinedText.contains("ios") ||
                combinedText.contains("mobile") || combinedText.contains("app") || combinedText.contains("flutter")
        val isPythonOrMl = combinedText.contains("python") || combinedText.contains("machine learning") ||
                combinedText.contains("ml") || combinedText.contains("ai") || combinedText.contains("vision") ||
                combinedText.contains("cv") || combinedText.contains("detection") || combinedText.contains("model") ||
                combinedText.contains("nlp") || combinedText.contains("symptom") || combinedText.contains("health")
        val isRealtimeOrChat = combinedText.contains("chat") || combinedText.contains("realtime") ||
                combinedText.contains("real-time") || combinedText.contains("collab") || combinedText.contains("message") ||
                combinedText.contains("notification")
        val isEcommerceOrDb = combinedText.contains("market") || combinedText.contains("store") ||
                combinedText.contains("e-commerce") || combinedText.contains("cart") || combinedText.contains("rent") ||
                combinedText.contains("pay") || combinedText.contains("book") || combinedText.contains("shop")
        val isIoTOrHardware = combinedText.contains("iot") || combinedText.contains("sensor") ||
                combinedText.contains("hardware") || combinedText.contains("arduino") || combinedText.contains("esp32") ||
                combinedText.contains("telemetry")
        val isDocOrRag = combinedText.contains("doc") || combinedText.contains("pdf") ||
                combinedText.contains("rag") || combinedText.contains("search") || combinedText.contains("summary") ||
                combinedText.contains("embed")

        val projectTitle = extractProjectTitle(prompt)

        // Generate tailored whyFitsProject for each tool across the complete A to Z catalog
        val allMarketTools = MarketToolCatalog.marketTools.map { tool ->
            val tailoredWhy = when (tool.id) {
                // Elite Code Generators (Paid)
                "ox_alpha" -> "High-efficiency autonomous coding engine that parses your entire repository to auto-generate complex fullstack modules for $projectTitle."
                "devin_ai" -> "Autonomous software engineer capable of setting up runtime environments, executing end-to-end features, and resolving complex compilation errors for $projectTitle."
                "cursor_pro" -> "Unlimited multi-file code editing powered by Claude 3.7 Sonnet & GPT-4o, accelerating core development of $projectTitle."
                "claude_code_cli" -> "Autonomous terminal agent that tests endpoints, debugs runtime logs, and refactors your codebase directly in your CLI."
                "augment_code" -> "Enterprise-grade contextual code intelligence tailored for large multi-service architectures and microservices."
                "bolt_new_pro" -> "Spins up an immediate in-browser fullstack sandbox for $projectTitle with zero local configuration needed."
                "replit_agent" -> "Transforms natural language descriptions into complete working application prototypes in minutes."
                "qodo_gen" -> "Generates unit tests and catches edge-case logic bugs in $projectTitle to ensure flawless live presentations."

                // Free Coding & Assist
                "cursor_free" -> "Provides AI tab-autocomplete and intelligent inline diffs to accelerate coding for $projectTitle."
                "github_copilot" -> "Free for students with GitHub Student Pack; completes repetitive boilerplate code across your stack."
                "windsurf" -> "Cascade engine tracks intent across your entire repository for cohesive refactoring and feature implementation."
                "continue_dev" -> "Open-source AI coding assistant in VS Code that connects to local Ollama models with zero subscription cost."
                "insomnia_rest" -> "Design, test, and debug all your REST & GraphQL API endpoints before connecting to your frontend UI."
                "playwright_test" -> "Automate end-to-end user journeys (login, form submission, checkout) with recorded video traces for faculty evaluation."
                "zod_validation" -> "Prevents runtime crashes by strictly validating user inputs and API payloads for $projectTitle."
                "typescript_lang" -> "Adds static type safety to your JavaScript stack, earning bonus marks for software engineering rigor."

                // Frontend
                "nextjs" -> if (!isMobile) "Top modern React 19 web framework with Server Components and SSR, creating a responsive web dashboard for $projectTitle." else "Ideal web admin companion dashboard for managing $projectTitle records."
                "flutter" -> if (isMobile) "Delivers a fast native iOS and Android application with fluid 60FPS UI from a single codebase for $projectTitle." else "Provides a high-performance cross-platform client app for $projectTitle."
                "expo_react_native" -> "Allows testing $projectTitle live on your real physical phone by scanning a QR code with the Expo Go app."
                "jetpack_compose" -> "Modern declarative Android UI toolkit in Kotlin with Material 3 design, highly regarded by Android evaluators."
                "shadcn_tailwind" -> "Provides clean, accessible, and themeable UI components tailored to modern web and mobile standards."
                "tailwind_css" -> "Utility-first CSS styling enabling rapid, responsive dark-mode styling for $projectTitle without context switching."
                "v0_dev" -> "Generates responsive, production-ready React components and forms tailored to $projectTitle from simple text prompts."
                "lovable_dev" -> "AI fullstack generator that creates frontend interfaces and database integrations in a visual web canvas."
                "astro_framework" -> "High-speed zero-JS baseline framework for blazing-fast project documentation portals and public showcases."
                "sveltekit" -> "Ultra-lightweight reactive framework with minimal boilerplate, perfect for team members new to complex state management."
                "vite_bundler" -> "Starts your local development server in under 200ms with instant Hot Module Replacement for rapid prototyping."
                "zustand_store" -> "Lightweight 1KB global state management for shopping carts, user authentication, and cached settings in $projectTitle."
                "yjs_collaboration" -> "Powers real-time multi-user document or canvas collaboration without merge conflicts for $projectTitle."

                // Backend & APIs
                "supabase_free" -> "Provides instant PostgreSQL database, user authentication, storage, and auto-generated REST/GraphQL APIs with zero backend setup."
                "fastapi" -> if (isPythonOrMl) "High-performance Python asynchronous API engine ideal for serving machine learning inferences and models for $projectTitle." else "Fast async Python REST backend with auto-generated Swagger documentation."
                "express_nodejs" -> "Classic Node.js REST API framework universally recognized by university faculty and easily extended with npm packages."
                "firebase_free" -> "Google Spark free tier delivers real-time database syncing, push notifications, and phone/email authentication."
                "convex_backend" -> "Reactive TypeScript backend where queries automatically push live updates to the frontend without WebSocket boilerplate."
                "clerk_auth" -> "Pre-built beautiful sign-in and user management modals that give $projectTitle a polished corporate aesthetic in 5 minutes."
                "better_auth" -> "Modern TypeScript-first auth library keeping user credentials inside your own database with zero third-party lock-in."
                "hono_framework" -> "Ultrafast edge-native web framework with sub-millisecond response times and zero dependencies."
                "appwrite_backend" -> "Open-source self-hostable BaaS with built-in Auth, Databases, Storage, and serverless Cloud Functions."
                "bun_runtime" -> "Executes TypeScript directly and installs packages 20x faster than npm, saving valuable development time."
                "ktor_framework" -> "Asynchronous Kotlin microservices framework sharing data classes directly with Android native apps."
                "resend_email" -> "Sends clean transactional verification emails and alerts for $projectTitle using React Email components."
                "stripe_payments" -> if (isEcommerceOrDb) "Powers realistic checkout flows with a 100% Free Sandbox Test Mode to simulate transactions during live reviews." else "Simulates secure payment and subscription processing using free developer test cards."
                "socket_io_realtime" -> if (isRealtimeOrChat) "Powers instant two-way chat messaging, live telemetry, and activity notifications for $projectTitle." else "Enables live real-time bidirectional communication between users and server."
                "uvicorn_asgi" -> "Lightning-fast ASGI web server with auto-reload for running FastAPI endpoints for $projectTitle."

                // Database & Storage
                "neon_postgres" -> "Serverless Postgres with instant branching, allowing team members to test database schema changes safely in isolated environments."
                "mongodb_atlas" -> "Flexible JSON document database with a generous free cloud cluster, perfect for unstructured data in $projectTitle."
                "planetscale" -> "Scalable MySQL database platform with automated schema migrations and zero downtime."
                "cloudflare_d1" -> "Serverless SQLite at the network edge with zero cold starts, querying data in under 15ms globally."
                "pinecone_vector" -> if (isDocOrRag || isPythonOrMl) "Stores high-dimensional vector embeddings for fast semantic similarity search and AI document retrieval in $projectTitle." else "Fully managed vector database for AI semantic search and recommendations."
                "qdrant_vector" -> "High-performance vector similarity search engine written in Rust with rich payload filtering for recommendation systems."
                "weaviate_vector" -> "Open-source vector database supporting hybrid search (keyword + vector) that can run offline via Docker."
                "redis_cache" -> "Sub-millisecond in-memory caching and session store, preventing API rate limit exhaustion and speeding up database queries."
                "upstash_serverless" -> "Serverless Redis & Vector database querying via standard HTTP, perfectly suited for serverless cloud functions."
                "prisma_orm" -> "Auto-generates type-safe database queries and includes Prisma Studio visual dashboard for easy data exploration."
                "meilisearch_fast" -> "Sub-50ms typo-tolerant search engine providing instant search-as-you-type user experience for $projectTitle."
                "influxdb_timeseries" -> if (isIoTOrHardware) "High-write throughput time-series database designed for streaming hardware sensor data in $projectTitle." else "Specialized time-series database for event metrics, analytics, and telemetry."
                "xata_database" -> "PostgreSQL data platform with a spreadsheet-like web UI and built-in full-text search."

                // Deploy & Cloud
                "vercel" -> "Instant 1-click cloud deployment with free custom SSL domains and preview URLs to demonstrate $projectTitle to professors."
                "render" -> "Free cloud hosting for Docker containers, web services, and cron jobs with automatic GitHub pushes."
                "railway_trial" -> "Visual deployment canvas with $5 free trial credits for spinning up fullstack multi-container stacks."
                "fly_io_deploy" -> "Lightweight global microVMs that run Docker containers close to users with built-in private networking."
                "docker_compose" -> "Containers bundle your entire stack into a single reproducible 'docker compose up' command for faculty review."
                "kubernetes_k8s" -> "Enterprise container orchestration demonstrating cloud-native DevOps readiness for top academic marks."
                "zeabur_cloud" -> "One-click deployment platform supporting fullstack services and databases with automatic subdomains."
                "aws_ec2_prod" -> "Industry-standard cloud infrastructure with a 12-month free tier for compute, storage, and networking."

                // AI Models & LLMs
                "gemini_flash" -> "Top free-tier AI model for college projects with 15 RPM free quota on Google AI Studio, powering fast reasoning for $projectTitle."
                "deepseek_r1" -> "Frontier open-weights model matching proprietary reasoning benchmarks; runnable locally or via ultra-low-cost APIs."
                "gpt_6_astra" -> "Flagship multimodal intelligence for intricate reasoning, image analysis, and complex problem-solving in $projectTitle."
                "claude_3_7_sonnet" -> "Premier reasoning and coding model with extended thinking mode, ideal for architecting complex algorithms."
                "groq_lpu" -> "Ultra-low latency LPU inference serving Llama and Mixtral models at 500+ tokens per second for real-time responsiveness."
                "ollama_local" -> "Runs open AI models 100% locally and offline on your laptop—immune to campus Wi-Fi drops during the viva demo."
                "huggingface_hub" -> "Hub for discovering pre-trained open models and hosting free live interactive Gradio/Streamlit demos on HF Spaces."
                "jina_ai_embeddings" -> "Extracts clean LLM-friendly markdown from any web link and generates multimodal embeddings for RAG systems."
                "llama_3_meta" -> "Meta's flagship open-weights models (8B & 70B) offering complete data privacy and zero API deprecation risk."
                "mistral_codestral" -> "High-efficiency code generation and fill-in-the-middle model optimized for programming tasks."
                "elevenlabs_ai" -> "Hyper-realistic voice synthesis and voiceover generation for interactive speaking avatars and demos."
                "xgboost_ml" -> if (isPythonOrMl) "Kaggle champion gradient boosting algorithm for predicting structured and tabular data in $projectTitle." else "High-performance gradient boosting library for tabular data analytics."
                "yolo_vision" -> if (isPythonOrMl || combinedText.contains("vision") || combinedText.contains("camera")) "State-of-the-art real-time object detection and segmentation running at 60+ FPS on webcam video." else "Real-time computer vision detection and pose estimation system."

                else -> tool.whyFitsProject
            }

            tool.copy(whyFitsProject = tailoredWhy)
        }

        // Rank the tools so the most contextually relevant are prioritized at the top,
        // while the user retains full access to the complete A to Z list!
        val scoredTools = allMarketTools.sortedByDescending { tool ->
            var score = 0
            val toolNameLower = tool.name.lowercase()
            val toolTaglineLower = tool.tagline.lowercase()

            // Domain boosts
            if (isMobile && (tool.id == "flutter" || tool.id == "expo_react_native" || tool.id == "jetpack_compose" || tool.id == "firebase_free")) score += 60
            if (isPythonOrMl && (tool.id == "fastapi" || tool.id == "xgboost_ml" || tool.id == "yolo_vision" || tool.id == "huggingface_hub" || tool.id == "langchain_langgraph")) score += 60
            if (isRealtimeOrChat && (tool.id == "socket_io_realtime" || tool.id == "convex_backend" || tool.id == "yjs_collaboration" || tool.id == "redis_cache")) score += 60
            if (isEcommerceOrDb && (tool.id == "stripe_payments" || tool.id == "neon_postgres" || tool.id == "supabase_free" || tool.id == "mongodb_atlas")) score += 60
            if (isIoTOrHardware && (tool.id == "influxdb_timeseries" || tool.id == "fastapi" || tool.id == "socket_io_realtime")) score += 60
            if (isDocOrRag && (tool.id == "pinecone_vector" || tool.id == "qdrant_vector" || tool.id == "jina_ai_embeddings" || tool.id == "weaviate_vector")) score += 60

            // Elite Code generators have high default baseline
            if (tool.id == "ox_alpha" || tool.id == "devin_ai" || tool.id == "cursor_free" || tool.id == "cursor_pro" || tool.id == "github_copilot" || tool.id == "bolt_new_pro") score += 40
            // Premier AI models
            if (tool.id == "gemini_flash" || tool.id == "claude_3_7_sonnet" || tool.id == "deepseek_r1" || tool.id == "ollama_local") score += 35
            // Standard frontend & deployment
            if (tool.id == "nextjs" || tool.id == "shadcn_tailwind" || tool.id == "tailwind_css" || tool.id == "vercel" || tool.id == "supabase_free") score += 30

            // Keyword matches from the prompt
            val keywords = combinedText.split("\\s+".toRegex()).filter { it.length > 3 }
            for (kw in keywords) {
                if (toolNameLower.contains(kw) || toolTaglineLower.contains(kw)) {
                    score += 8
                }
            }

            score
        }

        val archSummary = if (isMobile) {
            "Frontend: Flutter / Expo React Native | Backend: Supabase / Firebase | DB: Neon Postgres | AI: Gemini 3.5 & Ollama | Deploy: Vercel & Render"
        } else if (isPythonOrMl) {
            "Frontend: Next.js 19 & Tailwind | Backend: FastAPI (Python) | DB: Neon Postgres / Pinecone | AI: Gemini Flash & DeepSeek-R1 | Deploy: Vercel & Render"
        } else {
            "Frontend: Next.js 19 & Shadcn | Backend: Supabase Auth/API | DB: Neon Serverless Postgres | AI: Gemini 3.5 Flash | Deploy: Vercel"
        }

        return ProjectAnalysis(
            projectTitle = projectTitle,
            projectOverview = "Exhaustive A to Z developer tool recommendations covering the complete 2025/2026 market landscape. Includes both free-tier tools for $0 student budget and elite paid code generators for maximum efficiency.",
            recommendedArchitecture = archSummary,
            studentCostSummary = "Student Budget: $0.00 / month using Free Tiers & Student Packs (or Pro tier options for high-throughput teams).",
            tools = scoredTools
        )
    }

    private fun extractProjectTitle(prompt: String): String {
        val trimmed = prompt.trim()
        if (trimmed.length < 40) return trimmed.replaceFirstChar { it.uppercase() }
        val firstSentence = trimmed.split(".", "\n").firstOrNull()?.trim() ?: trimmed
        return if (firstSentence.length <= 45) {
            firstSentence.replaceFirstChar { it.uppercase() }
        } else {
            firstSentence.take(40).trim() + "..."
        }
    }
}
