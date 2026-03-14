# Lumina Agentification Transformation Manual

## 1. Vision & Architecture

### 1.1 Goal
Transform **Lumina** from a reactive "Visual Aid App" into a proactive **"Personal Visual Agent"**.
The Agent shall possess:
- **Cognition**: Understanding the semantic meaning of scenes, not just labels.
- **Memory**: Remembering context, user preferences, and spatial history.
- **Planning**: Breaking down complex user goals into actionable steps.
- **Action**: Interacting with the digital and physical world (via APIs).

### 1.2 Edge-Cloud Hybrid Architecture
To balance real-time safety with deep intelligence, we adopt a hybrid architecture:

*   **Edge (Fast System / "Little Brain")**:
    *   **Role**: Real-time obstacle avoidance, basic object detection, motion tracking.
    *   **Tech**: Android Native, YOLOv8 (ONNX Runtime), CameraX.
    *   **Latency**: < 100ms.
    *   **Privacy**: All raw video streams are processed locally; only specific keyframes are uploaded.

*   **Cloud (Deep System / "Big Brain")**:
    *   **Role**: Complex scene understanding, VQA (Visual Question Answering), logical reasoning, planning.
    *   **Tech**: LLM/VLM (Gemini 1.5 Pro/Flash, GPT-4o), Vector Database.
    *   **Latency**: 1s - 5s.

## 2. Perception Layer Upgrade

### 2.1 From Object Detection to Scene Understanding
Current YOLOv8 provides bounding boxes (`[Person, 2.5m]`). This is insufficient for "Is it safe to cross the road?".

**Strategy:**
1.  **Event Triggering**: When YOLO detects complex scenarios (e.g., traffic lights, crowds) or User asks a question.
2.  **Keyframe Extraction**: Capture the current high-res frame.
3.  **VLM Inference**: Send the frame to a Multimodal LLM (VLM).
    *   *Prompt*: "Describe the scene in front of the blind user. Is there any immediate danger? What is the status of the traffic light?"

### 2.2 Spatial Awareness
*   **Depth Estimation**: Integrate monocular depth estimation (e.g., MiDaS on Edge) to provide a dense depth map, offering granular distance feedback beyond simple bounding boxes.

## 3. Memory Layer

### 3.1 Short-term Memory (Context)
*   **Mechanism**: Maintain a sliding window of recent observations (last 10-30 seconds).
*   **Use Case**: User asks "What did I just pass by?". Agent retrieves recent VLM descriptions.

### 3.2 Long-term Memory (Vector DB)
*   **Mechanism**: Store embeddings of significant locations, faces, and user preferences.
*   **Tech**: **ChromaDB** (embedded) or **Pinecone** (cloud).
*   **Use Case**:
    *   "Take me to my favorite coffee shop." (Retrieves location visual features).
    *   "Who is standing in front of me?" (Matches face embeddings with contacts).

## 4. Planning & Decision Layer

### 4.1 The Brain (LLM)
The LLM acts as the central controller (Orchestrator).
*   **Input**: User voice command + VLM scene description + Memory context.
*   **Output**: Action plan or Voice response.

### 4.2 Function Calling (Tools)
Give the Agent "hands" to use Android system capabilities.
*   `call_contact(name)`
*   `get_current_location()`
*   `search_map(destination)`
*   `save_memory(content)`

**Example Flow**:
1.  **User**: "Call a taxi to go home."
2.  **Agent (Thought)**: User wants a taxi. Destination is "home". I need current location.
3.  **Agent (Action)**: Calls `get_current_location()`.
4.  **Agent (Action)**: Calls `uber_api.request_ride(from=current, to=home_address)`.
5.  **Agent (Response)**: "I've requested a ride. It will arrive in 5 minutes."

## 5. Interaction Layer

### 5.1 Natural Language Interface
*   **STT (Speech-to-Text)**: Upgrade to OpenAI Whisper or on-device Google Speech Recognition for better accuracy.
*   **TTS (Text-to-Speech)**: Use natural, emotional voices (e.g., ElevenLabs or Gemini TTS).

### 5.2 Proactive Feedback
Instead of passive waiting, the Agent proactively notifies:
*   "You seem lost, do you need help?"
*   "That looks like the bus 42 you usually take."

## 6. Recommended Tech Stack

| Component | Recommendation | Alternative |
| :--- | :--- | :--- |
| **LLM / VLM** | **Gemini 1.5 Flash** (Fast, Cheap, Multimodal) | GPT-4o, Claude 3.5 Sonnet |
| **Orchestration** | **LangChain4j** (Java/Kotlin friendly) | Semantic Kernel |
| **Vector DB** | **ChromaDB** (Local/Server) | Pinecone, Milvus |
| **STT / TTS** | **Android Native** (Cost effective) | OpenAI Whisper / ElevenLabs |
| **Edge AI** | **ONNX Runtime** (Current) | TensorFlow Lite |

## 7. Implementation Roadmap

### Phase 1: The "Talking" Eye (VLM Integration)
- [ ] Integrate Gemini 1.5 Flash API.
- [ ] Implement "Snap & Ask" feature: User asks question -> Upload Image -> TTS Answer.

### Phase 2: The Memory (RAG)
- [ ] Set up a local database (Room/SQLite) for chat history.
- [ ] Integrate a Vector DB for semantic search of past events.

### Phase 3: The Agent (Tools)
- [ ] Implement LangChain4j.
- [ ] Define basic tools (Location, Battery, Time).
- [ ] Connect LLM to Tools.

### Phase 4: Full Autonomy
- [ ] Continuous video stream analysis (sampled).
- [ ] Proactive safety alerts based on VLM understanding.
