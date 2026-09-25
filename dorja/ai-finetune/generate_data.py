#!/usr/bin/env python3
"""Generate fine-tuning data for the DORJA Needle tool surface.

Emits both Needle data formats from the same examples:

  data/local/train.jsonl      {"query", "tools", "answers"}  -> needle finetune
  data/platform/train.jsonl   OpenAI chat "messages"+"tools" -> needle platform finetune

Rules followed (github.com/cactus-compute/needle llms.txt):
  - arguments contain only values evidenced in the query text
  - optional fields with no evidence are omitted, never guessed
  - ~1 in 6 examples is off-topic with an empty answers list
  - closed sets (intent, topic) are enums; numbers stay within tool bounds

Run:  python3 generate_data.py
"""

import json
import os
import random

HERE = os.path.dirname(os.path.abspath(__file__))
random.seed(20260926)  # reproducible splits

AREAS = {
    "en": ["Gulshan", "Dhanmondi", "Bandra", "Andheri", "Karachi", "Milan"],
    "bn": ["গুলশান", "ধানমন্ডি", "বন্দরা", "আন্ধেরি", "করাচি", "মিলান"],
    "hi": ["गुलशन", "धानमंडी", "बांद्रा", "अंधेरी", "कराची", "मिलान"],
    "ur": ["گلشن", "دھن مونڈی", "باندھرا", "اندھیری", "کراچی", "میلاں"],
    "it": ["Gulshan", "Dhanmondi", "Bandra", "Andheri", "Karachi", "Milano"],
}
PRICES = [8000, 12000, 15000, 25000, 35000, 60000, 120000, 4500000]
BEDS = [1, 2, 3, 4, 5]
LISTING_IDS = ["l1", "l2", "l3", "l7", "l12", "l23"]

OFF_TOPIC = {
    "en": ["what's the weather in Lagos", "tell me a joke", "who won the match yesterday",
           "play some music", "set an alarm for 7 am"],
    "bn": ["লাগোসের আবহাওয়া কেমন", "একটা জোক বলো", "গতকালের ম্যাচ কে জিতেছে",
           "গান বাজাও", "সকাল ৭টায় অ্যালার্ম দাও"],
    "hi": ["लागोस का मौसम कैसा है", "कोई चुटकुला सुनाओ", "कल का मैच कौन जीता",
           "कुछ संगीत चलाओ", "सुबह 7 बजे का अलार्म लगाओ"],
    "ur": ["لاگوس کی موسم کیسی ہے", "کوئی لطیفہ سناؤ", "کل کا میچ کون جیتا",
           "کوئی موسیقی چلاؤ", "صبح 7 بجے کا الارم لگاؤ"],
    "it": ["com'è il tempo a Lagos", "raccontami una barzelletta", "chi ha vinto la partita",
           "metti della musica", "metti una sveglia alle 7"],
}

def search_queries():
    """(lang, query, expected_arguments_or_None) — None means off-topic."""
    for lang in ("en", "bn", "hi", "ur", "it"):
        area = random.choice(AREAS[lang])
        price = random.choice(PRICES)
        beds = random.choice(BEDS)
        yield (lang, {
            "en": f"find places for rent in {area} under {price}",
            "bn": f"{area} এ {price} এর নিচে ভাড়ার জায়গা খুঁজো",
            "hi": f"{area} में {price} से कम किराए की जगहें खोजो",
            "ur": f"{area} میں {price} سے کم کرائے کی جگہیں تلاش کرو",
            "it": f"trova posti in affitto a {area} sotto {price}",
        }[lang], {"intent": "RENT", "area": area, "maxPrice": price})
        yield (lang, {
            "en": f"show houses for sale in {area} with at least {beds} bedrooms",
            "bn": f"{area} এ অন্তত {beds} বেডরুমের বিক্রির বাড়ি দেখাও",
            "hi": f"{area} में कम से कम {beds} बेडरूम वाले बिक्री के घर दिखाओ",
            "ur": f"{area} میں کم از کم {beds} بیڈ رومز والے فروخت گھر دکھاؤ",
            "it": f"mostra case in vendita a {area} con almeno {beds} camere",
        }[lang], {"intent": "SALE", "area": area, "minBedrooms": beds})
        yield (lang, {
            "en": f"any rentals in {area}?",
            "bn": f"{area} এ কোনো ভাড়া আছে?",
            "hi": f"{area} में कोई किराया है?",
            "ur": f"{area} میں کوئی کرایہ ہے؟",
            "it": f"ci sono affitti a {area}?",
        }[lang], {"intent": "RENT", "area": area})

def details_queries():
    for lang in ("en", "bn", "hi", "ur", "it"):
        lid = random.choice(LISTING_IDS)
        yield (lang, {
            "en": f"tell me everything about listing {lid}",
            "bn": f"{lid} লিস্টিং সম্পর্কে সব বলো",
            "hi": f"{lid} लिस्टिंग के बारे में सब बताओ",
            "ur": f"{lid} لسٹنگ کے بارے میں سب بتاؤ",
            "it": f"dimmi tutto sull'annuncio {lid}",
        }[lang], {"listingId": lid})

def count_queries():
    for lang in ("en", "bn", "hi", "ur", "it"):
        area = random.choice(AREAS[lang])
        yield (lang, {
            "en": f"how many places are listed in {area}?",
            "bn": f"{area} এ কতগুলো জায়গা লিস্ট করা আছে?",
            "hi": f"{area} में कितनी जगहें लिस्ट हैं?",
            "ur": f"{area} میں کتنی جگہیں درج ہیں؟",
            "it": f"quanti annunci ci sono a {area}?",
        }[lang], {"area": area})
        yield (lang, {
            "en": "count all the properties for sale",
            "bn": "বিক্রির সব প্রপার্টি গণনা করো",
            "hi": "बिक्री के लिए सभी प्रॉपर्टी गिनो",
            "ur": "فروخت کی تمام پراپرٹیز گنیں",
            "it": "conta tutte le proprietà in vendita",
        }[lang], {"intent": "SALE"})

SUPPORT_TOPICS = {
    "verification": {
        "en": "how does identity verification work",
        "bn": "পরিচয় যাচাই কীভাবে কাজ করে",
        "hi": "पहचान सत्यापन कैसे काम करता है",
        "ur": "شناختی تصدیق کیسے کام کرتی ہے",
        "it": "come funziona la verifica dell'identità",
    },
    "visits": {
        "en": "how do I book a visit",
        "bn": "ভিজিট বুক করব কীভাবে",
        "hi": "विज़िट कैसे बुक करें",
        "ur": "وزٹ کیسے بک کریں",
        "it": "come prenoto una visita",
    },
    "scans": {
        "en": "what is a 3D scan",
        "bn": "3ডি স্ক্যান কী",
        "hi": "3D स्कैन क्या है",
        "ur": "3D سکین کیا ہے",
        "it": "cos'è una scansione 3D",
    },
    "compare": {
        "en": "how can I compare two properties",
        "bn": "দুটি প্রপার্টি কীভাবে তুলনা করব",
        "hi": "दो प्रॉपर्टी की तुलना कैसे करें",
        "ur": "دو پراپرٹیز کا موازنہ کیسے کریں",
        "it": "come confronto due proprietà",
    },
    "general": {
        "en": "what can this app do",
        "bn": "এই অ্যাপ কী কী করতে পারে",
        "hi": "यह ऐप क्या कर सकता है",
        "ur": "یہ ایپ کیا کر سکتی ہے",
        "it": "cosa può fare questa app",
    },
}

def support_queries():
    for lang in ("en", "bn", "hi", "ur", "it"):
        for topic, texts in SUPPORT_TOPICS.items():
            yield (lang, texts[lang], {"topic": topic})

def build_examples():
    examples = []
    for lang, query, args in search_queries():
        examples.append(("search_listings", lang, query, args))
    for lang, query, args in details_queries():
        examples.append(("get_listing_details", lang, query, args))
    for lang, query, args in count_queries():
        examples.append(("count_listings", lang, query, args))
    for lang, query, args in support_queries():
        examples.append(("app_support", lang, query, args))

    # ~1 in 6 off-topic refusals, spread over languages.
    langs = ["en", "bn", "hi", "ur", "it"]
    for i in range(max(12, len(examples) // 6)):
        lang = langs[i % len(langs)]
        examples.append((None, lang, random.choice(OFF_TOPIC[lang]), None))
    return examples

def load_tools():
    with open(os.path.join(HERE, "tools.json"), encoding="utf-8") as f:
        return json.load(f)

def local_line(query, answers):
    return {"query": query, "tools": TOOLS, "answers": answers or []}

def chat_line(query, answers):
    """OpenAI chat format; tool_calls arguments are a JSON string."""
    assistant_msg = {"role": "assistant", "content": None}
    if answers:
        assistant_msg["tool_calls"] = [
            {
                "type": "function",
                "function": {
                    "name": name,
                    "arguments": json.dumps(args, ensure_ascii=False),
                },
            }
            for (name, args) in answers
        ]
    return {
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": query},
            assistant_msg,
        ],
        "tools": TOOLS,
    }

SYSTEM_PROMPT = (
    "You are DORJA's property assistant. Pick exactly one tool for the user's "
    "request and fill its arguments. If no tool matches the request, return no "
    "tool call. Never invent listing ids; only use ids from earlier results."
)

TOOLS = load_tools()

def main():
    examples = build_examples()
    random.shuffle(examples)
    n = len(examples)
    val_n = max(2, int(n * 0.1))
    test_n = max(2, int(n * 0.1))
    splits = {
        "train": examples[: n - val_n - test_n],
        "validation": examples[n - val_n - test_n : n - test_n],
        "test": examples[n - test_n :],
    }

    out_local = os.path.join(HERE, "data", "local")
    out_platform = os.path.join(HERE, "data", "platform")
    os.makedirs(out_local, exist_ok=True)
    os.makedirs(out_platform, exist_ok=True)

    for split, items in splits.items():
        with open(os.path.join(out_local, f"{split}.jsonl"), "w", encoding="utf-8") as f:
            for tool, _lang, query, args in items:
                answers = [{"name": tool, "arguments": args}] if tool else []
                f.write(json.dumps(local_line(query, answers), ensure_ascii=False) + "\n")
        with open(os.path.join(out_platform, f"{split}.jsonl"), "w", encoding="utf-8") as f:
            for tool, _lang, query, args in items:
                answers = [(tool, args)] if tool else []
                f.write(json.dumps(chat_line(query, answers), ensure_ascii=False) + "\n")

    off = sum(1 for t, *_ in examples if t is None)
    print(f"examples: {n} total ({off} off-topic refusals, {100 * off // n}%)")
    print(f"train={len(splits['train'])} validation={len(splits['validation'])} test={len(splits['test'])}")
    print(f"wrote {out_local}/*.jsonl and {out_platform}/*.jsonl")

if __name__ == "__main__":
    main()
