import os, re

candidates = set()
for root, _, files in os.walk('c:/Users/jusch/Documents/antigravity/blissful-hertz/src/main'):
    for file in files:
        if file.endswith('.java'):
            with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                content = f.read()
                strings = re.findall(r'\"([^\"]*)\"', content)
                for s in strings:
                    words = re.findall(r'\b[a-zA-Z]*(?:ae|oe|ue|Ae|Oe|Ue)[a-zA-Z]*\b', s)
                    for w in words:
                        candidates.add(w)

with open('candidates.txt', 'w', encoding='utf-8') as f:
    for c in sorted(list(candidates)):
        f.write(c + '\n')
print("Done")
