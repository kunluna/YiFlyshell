# Agnes Image API 使用文档 & Key

## 来源
老板 2026-06-29 22:31 给的指令：
> https://agnes-ai.com/zh-Hans/docs/agnes-image-21-flash 你把这个文档给密蒙看，让他自己看看怎么去调用这个生图的技能，后面的是这个 API key。sk-uUY…m25E

## 文档
- https://agnes-ai.com/zh-Hans/docs/agnes-image-21-flash
- 给 MIMO 看，让他按文档调用

## API Key
- Key: sk-uUY…m25E
- 硬编码，不能截断
- 用途：agnes-image-2.1-flash 图生图

## API 调用方式
```
POST https://apihub.agnes-ai.com/v1/images/generations
Authorization: Bearer sk-uUY…m25E
Content-Type: application/json

{
  "model": "agnes-image-2.1-flash",
  "prompt": "...",
  "size": "512x512",
  "extra_body": {
    "image": ["data:image/png;base64,..."],
    "response_format": "url"
  }
}
```