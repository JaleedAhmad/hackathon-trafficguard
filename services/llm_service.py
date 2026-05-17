import os
from google import genai
from google.genai import types
import ollama
from services.logger import log_agent_event

api_key = os.getenv("GEMINI_API_KEY", "")
# Instantiate client gracefully if API key is not fully configured
client = genai.Client(api_key=api_key) if api_key and api_key != "your_gemini_api_key_here" else genai.Client(api_key="mock")

async def call_llm(prompt: str, model: str = "gemini-2.5-pro", require_json: bool = False) -> str:
    try:
        config_kwargs = {}
        if require_json:
            config_kwargs["response_mime_type"] = "application/json"
            
        config = types.GenerateContentConfig(**config_kwargs) if config_kwargs else None
        
        response = client.models.generate_content(
            model=model,
            contents=prompt,
            config=config
        )
        log_agent_event("LLM_CALL", {
            "provider": "google-genai",
            "model": model,
            "status": "success"
        })
        return response.text
    except Exception as e:
        log_agent_event("FALLBACK_TRIGGERED", {
            "reason": str(e),
            "switching_to": "ollama_cloud",
            "model": "gpt-oss:120b-cloud"
        })
        ollama_client = ollama.Client(
            host="https://ollama.com",
            headers={"Authorization": f"Bearer {os.getenv('OLLAMA_API_KEY', '')}"}
        )
        response = ollama_client.chat(
            model="gpt-oss:120b-cloud",
            messages=[{"role": "user", "content": prompt}]
        )
        log_agent_event("LLM_CALL", {
            "provider": "ollama_cloud",
            "model": "gpt-oss:120b-cloud",
            "status": "success"
        })
        return response["message"]["content"]
