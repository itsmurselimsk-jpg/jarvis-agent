import os

base = "app/src/main/java/com/jarvis/ai"

for root, dirs, files in os.walk(base):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            
            rel = os.path.relpath(root, base)
            if rel == ".":
                expected_pkg = "com.jarvis.ai"
            else:
                expected_pkg = "com.jarvis.ai." + rel.replace(os.sep, ".")

            lines = content.splitlines()
            clean_lines = []
            for line in lines:
                if line.startswith("package "):
                    continue
                if "com.example" in line:
                    line = line.replace("com.example", "com.jarvis.ai")
                clean_lines.append(line)

            clean_lines.insert(0, "package " + expected_pkg)

            full_str = "\n".join(clean_lines)
            imports_to_add = []
            if "JarvisApp" in full_str and "import com.jarvis.ai.ui.JarvisApp" not in full_str and expected_pkg != "com.jarvis.ai.ui":
                imports_to_add.append("import com.jarvis.ai.ui.JarvisApp")
            if "JarvisViewModel" in full_str and "import com.jarvis.ai.ui.JarvisViewModel" not in full_str and expected_pkg != "com.jarvis.ai.ui":
                imports_to_add.append("import com.jarvis.ai.ui.JarvisViewModel")
            if "SubScreen" in full_str and "import com.jarvis.ai.ui.SubScreen" not in full_str and expected_pkg != "com.jarvis.ai.ui":
                imports_to_add.append("import com.jarvis.ai.ui.SubScreen")
            if "NavTab" in full_str and "import com.jarvis.ai.ui.NavTab" not in full_str and expected_pkg != "com.jarvis.ai.ui":
                imports_to_add.append("import com.jarvis.ai.ui.NavTab")
            if any(c in full_str for c in ["HolographicCoreHero", "HolographicJarvisCore3D", "CodeStudioView", "JarvisOverlayHud", "ArcReactorEmblem", "AudioVisualizer"]) and "import com.jarvis.ai.ui.components.*" not in full_str and expected_pkg != "com.jarvis.ai.ui.components":
                imports_to_add.append("import com.jarvis.ai.ui.components.*")

            if imports_to_add:
                pkg_i = 0
                for idx, l in enumerate(clean_lines):
                    if l.startswith("package "):
                        pkg_i = idx
                        break
                insert_idx = pkg_i + 1
                while insert_idx < len(clean_lines) and clean_lines[insert_idx].strip() == "":
                    insert_idx += 1
                clean_lines.insert(insert_idx, "\n".join(imports_to_add))

            new_content = "\n".join(clean_lines) + "\n"
            if new_content != content:
                with open(path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"Corrected package for {path} -> {expected_pkg}")
