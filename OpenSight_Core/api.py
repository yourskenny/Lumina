import uvicorn
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.openapi.docs import get_swagger_ui_html
from pydantic import BaseModel
import numpy as np
import cv2
import threading
import base64
import json

app = FastAPI(title="Lumina OpenSight API", docs_url=None, redoc_url=None)

# Allow CORS for frontend access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Custom Swagger UI for better connectivity in China
@app.get("/docs", include_in_schema=False)
async def custom_swagger_ui_html():
    return get_swagger_ui_html(
        openapi_url=app.openapi_url,
        title=app.title + " - Swagger UI",
        oauth2_redirect_url=app.swagger_ui_oauth2_redirect_url,
        swagger_js_url="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui-bundle.js",
        swagger_css_url="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui.css",
    )

# Global engine instance
_engine = None
_engine_lock = threading.Lock()

def get_engine():
    global _engine
    if _engine is None:
        with _engine_lock:
            if _engine is None:
                from blind_navigator import BlindNavigator
                print("[API] Initializing BlindNavigator...")
                _engine = BlindNavigator()
    return _engine

class ImagePayload(BaseModel):
    image_base64: str

@app.get("/health")
def health_check():
    return {"status": "ok", "engine_ready": _engine is not None}

@app.post("/analyze")
async def analyze_scene(
    file: UploadFile = File(None),
    payload: ImagePayload = None
):
    """
    Analyze a scene from an image.
    Supports multipart/form-data (file) or JSON (base64).
    """
    img = None
    
    # 1. Try file upload
    if file:
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    
    # 2. Try JSON payload
    elif payload and payload.image_base64:
        try:
            raw = base64.b64decode(payload.image_base64)
            nparr = np.frombuffer(raw, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Invalid base64: {str(e)}")
            
    if img is None:
        raise HTTPException(status_code=400, detail="No valid image provided")
        
    try:
        engine = get_engine()
        result = engine.analyze_scene(img)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")

if __name__ == "__main__":
    # Run with uvicorn
    # host="0.0.0.0" allows access from external devices (e.g. mobile app)
    uvicorn.run(app, host="0.0.0.0", port=5000)

