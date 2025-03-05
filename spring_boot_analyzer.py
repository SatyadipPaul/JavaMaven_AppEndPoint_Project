#!/usr/bin/env python3
"""
Spring Boot Project Analyzer

This script analyzes the structure of a Spring Boot project and displays it as a hierarchical tree
with special highlighting for Spring Boot components.

Usage:
    python spring_boot_analyzer.py [options] [directory]

Options:
    --exclude-file=FILENAME         Exclude specific file by name
    --exclude-ext=EXTENSION         Exclude files by extension (without dot)
    --exclude-dir=DIRECTORY         Exclude directory
    --exclude-pattern=PATTERN       Exclude by pattern (glob syntax)
    --output=FORMAT                 Output format (text, json) [default: text]
    --color=BOOL                    Enable/disable color output [default: true]
    --max-depth=DEPTH               Maximum depth to scan [default: no limit]

Examples:
    # Analyze current directory
    python spring_boot_analyzer.py

    # Analyze specific directory
    python spring_boot_analyzer.py /path/to/spring/project

    # Exclude additional files or directories
    python spring_boot_analyzer.py --exclude-file=custom.properties --exclude-dir=my-tmp-dir

    # Output as JSON
    python spring_boot_analyzer.py --output=json
"""

import os
import sys
import re
import fnmatch
import json
import argparse
import xml.etree.ElementTree as ET
from collections import defaultdict
import subprocess
from pathlib import Path
from typing import List, Dict, Set, Tuple, Optional, Any, Union


# ANSI color codes
class Colors:
    RESET = "\033[0m"
    BOLD = "\033[1m"
    RED = "\033[91m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    BLUE = "\033[94m"
    MAGENTA = "\033[95m"
    CYAN = "\033[96m"
    WHITE = "\033[97m"


class SpringBootAnalyzer:
    # Spring Boot component patterns
    PATTERNS = {
        "controller": re.compile(r'@(Rest)?Controller|@RequestMapping'),
        "service": re.compile(r'@Service'),
        "repository": re.compile(r'@Repository|@Dao'),
        "entity": re.compile(r'@Entity|@Table|@Document'),
        "config": re.compile(r'@Configuration|@EnableAutoConfiguration|@ComponentScan'),
        "component": re.compile(r'@Component|@Bean'),
    }

    def __init__(self, 
                 root_dir: str = ".", 
                 exclude_files: List[str] = None,
                 exclude_exts: List[str] = None, 
                 exclude_dirs: List[str] = None,
                 exclude_patterns: List[str] = None,
                 max_depth: int = -1,
                 use_color: bool = True):
        """
        Initialize the Spring Boot project analyzer.
        
        Args:
            root_dir: Root directory of the Spring Boot project
            exclude_files: List of filenames to exclude
            exclude_exts: List of file extensions to exclude (without dot)
            exclude_dirs: List of directories to exclude
            exclude_patterns: List of glob patterns to exclude
            max_depth: Maximum depth for directory scanning (-1 for unlimited)
            use_color: Whether to use colored output
        """
        self.root_dir = os.path.abspath(root_dir)
        self.exclude_files = set(exclude_files or [])
        self.exclude_exts = set(exclude_exts or [])
        self.exclude_dirs = set(exclude_dirs or [])
        self.exclude_patterns = set(exclude_patterns or [])
        self.max_depth = max_depth
        self.use_color = use_color
        
        # Add default Spring Boot exclusions
        self._add_default_exclusions()
        
        # Stats for summary
        self.stats = defaultdict(int)
        self.jdk_version = "Unknown"
        self.spring_boot_version = "Unknown"
        self.project_type = "Unknown"
        
        # Tree structure for the project
        self.project_tree = {"name": os.path.basename(self.root_dir), "type": "dir", "children": []}

    def _add_default_exclusions(self):
        """Add default exclusions for Spring Boot projects"""
        # Build artifacts
        self.exclude_dirs.update(["target", "build", "bin", "out"])
        self.exclude_exts.update(["class", "jar", "war"])
        
        # IDE configurations
        self.exclude_dirs.update([".idea", ".vscode", ".eclipse", ".settings", ".metadata"])
        self.exclude_exts.update(["iml", "iws", "ipr", "project", "classpath"])
        
        # Local configuration files
        self.exclude_files.update(["application-local.properties", "application-dev.properties"])
        
        # Logs and temporary files
        self.exclude_dirs.update(["logs", "tmp"])
        self.exclude_exts.update(["log"])
        self.exclude_patterns.update(["*.log", "tmp*"])
        
        # Dependency directories
        self.exclude_dirs.update(["node_modules", ".mvn", "gradle"])
        
        # VCS directories
        self.exclude_dirs.update([".git", ".svn", ".hg"])

    def _is_excluded(self, path: str, is_dir: bool = False) -> bool:
        """
        Check if a file or directory should be excluded.
        
        Args:
            path: Path to check
            is_dir: Whether the path is a directory
            
        Returns:
            True if the path should be excluded, False otherwise
        """
        name = os.path.basename(path)
        
        # Check for excluded directories
        if is_dir and name in self.exclude_dirs:
            return True
            
        # For files, check excluded files, extensions, and patterns
        if not is_dir:
            if name in self.exclude_files:
                return True
                
            ext = os.path.splitext(name)[1].lstrip('.')
            if ext in self.exclude_exts:
                return True
                
        # Check patterns for both files and directories
        for pattern in self.exclude_patterns:
            if fnmatch.fnmatch(name, pattern):
                return True
                
        return False

    def _detect_jdk_version(self):
        """Detect the JDK version from the system or build files"""
        try:
            # First try to detect from build files
            # Check Maven pom.xml
            pom_path = os.path.join(self.root_dir, "pom.xml")
            if os.path.exists(pom_path):
                try:
                    tree = ET.parse(pom_path)
                    root = tree.getroot()
                    
                    # Extract namespace
                    m = re.match(r'\{(.*)\}', root.tag)
                    ns = {'ns': m.group(1)} if m else {}
                    
                    # Try to find Java version in properties
                    properties = root.find(".//ns:properties", ns)
                    if properties is not None:
                        java_version = properties.find("./ns:java.version", ns)
                        if java_version is not None and java_version.text:
                            self.jdk_version = java_version.text
                            return
                            
                        maven_compiler_source = properties.find("./ns:maven.compiler.source", ns)
                        if maven_compiler_source is not None and maven_compiler_source.text:
                            self.jdk_version = maven_compiler_source.text
                            return
                except Exception as e:
                    pass  # Silently continue if pom.xml parsing fails
            
            # Check Gradle build.gradle
            gradle_path = os.path.join(self.root_dir, "build.gradle")
            if os.path.exists(gradle_path):
                with open(gradle_path, 'r') as f:
                    content = f.read()
                    # Look for sourceCompatibility or targetCompatibility
                    matches = re.search(r'(sourceCompatibility|targetCompatibility)\s*=\s*[\'"]?(\d+(\.\d+)*)[\'"]?', content)
                    if matches:
                        self.jdk_version = matches.group(2)
                        return
            
            # If no build file info, try system Java
            try:
                result = subprocess.run(['java', '-version'], capture_output=True, text=True, stderr=subprocess.STDOUT)
                version_output = result.stdout or result.stderr
                matches = re.search(r'version\s+"(\d+(\.\d+)*)', version_output)
                if matches:
                    self.jdk_version = matches.group(1)
                    return
            except Exception:
                pass
                
        except Exception as e:
            print(f"Error detecting JDK version: {e}", file=sys.stderr)
            
        self.jdk_version = "Unknown (JDK detection failed)"

    def _detect_spring_boot_version(self):
        """Detect the Spring Boot version from build files"""
        try:
            # Try Maven pom.xml first
            pom_path = os.path.join(self.root_dir, "pom.xml")
            if os.path.exists(pom_path):
                try:
                    tree = ET.parse(pom_path)
                    root = tree.getroot()
                    
                    # Extract namespace
                    m = re.match(r'\{(.*)\}', root.tag)
                    ns = {'ns': m.group(1)} if m else {}
                    
                    # Try to find Spring Boot parent
                    parent = root.find("./ns:parent", ns)
                    if parent is not None:
                        group_id = parent.find("./ns:groupId", ns)
                        artifact_id = parent.find("./ns:artifactId", ns)
                        
                        if (group_id is not None and group_id.text == "org.springframework.boot" and
                            artifact_id is not None and artifact_id.text == "spring-boot-starter-parent"):
                            version = parent.find("./ns:version", ns)
                            if version is not None and version.text:
                                self.spring_boot_version = version.text
                                return
                    
                    # Look in dependencies
                    dependencies = root.findall(".//ns:dependency", ns)
                    for dep in dependencies:
                        group_id = dep.find("./ns:groupId", ns)
                        artifact_id = dep.find("./ns:artifactId", ns)
                        
                        if (group_id is not None and group_id.text == "org.springframework.boot" and
                            artifact_id is not None and "spring-boot" in artifact_id.text):
                            version = dep.find("./ns:version", ns)
                            if version is not None and version.text:
                                self.spring_boot_version = version.text
                                return
                                
                except Exception as e:
                    pass  # Silently continue if pom.xml parsing fails
            
            # Check Gradle build.gradle
            gradle_path = os.path.join(self.root_dir, "build.gradle")
            if os.path.exists(gradle_path):
                with open(gradle_path, 'r') as f:
                    content = f.read()
                    # Look for Spring Boot version
                    matches = re.search(r'spring-boot[\'"]?\s*:\s*[\'"]?(\d+\.\d+\.\d+)[\'"]?', content)
                    if matches:
                        self.spring_boot_version = matches.group(1)
                        return
                        
                    matches = re.search(r'org\.springframework\.boot[\'"]?\s*version\s*[\'"]?(\d+\.\d+\.\d+)[\'"]?', content)
                    if matches:
                        self.spring_boot_version = matches.group(1)
                        return
                    
        except Exception as e:
            print(f"Error detecting Spring Boot version: {e}", file=sys.stderr)
            
        self.spring_boot_version = "Unknown (version detection failed)"

    def _detect_file_type(self, file_path: str) -> str:
        """
        Detect the type of a file based on its content and path.
        
        Args:
            file_path: Path to the file
            
        Returns:
            File type as a string
        """
        filename = os.path.basename(file_path)
        ext = os.path.splitext(filename)[1].lower()
        
        # Check if it's a resource file
        if filename in ["application.properties", "application.yml", "application.yaml", 
                        "bootstrap.properties", "bootstrap.yml", "bootstrap.yaml"]:
            self.stats["resources"] += 1
            return "resource"
            
        if ext not in [".java", ".kt", ".groovy"]:
            # Not a Java/Kotlin/Groovy file, no need to check content
            return "file"
            
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
                # Check for Spring Boot components
                for component_type, pattern in self.PATTERNS.items():
                    if pattern.search(content):
                        self.stats[component_type] += 1
                        return component_type
                        
        except Exception as e:
            # If there's any error reading the file, just return generic file type
            return "file"
            
        return "file"

    def _detect_project_type(self):
        """Detect the type of Spring Boot project"""
        has_web = False
        has_data = False
        has_cloud = False
        has_security = False
        has_reactive = False
        
        # Check pom.xml first
        pom_path = os.path.join(self.root_dir, "pom.xml")
        if os.path.exists(pom_path):
            try:
                tree = ET.parse(pom_path)
                root = tree.getroot()
                
                # Extract namespace
                m = re.match(r'\{(.*)\}', root.tag)
                ns = {'ns': m.group(1)} if m else {}
                
                # Check dependencies
                dependencies = root.findall(".//ns:dependency", ns)
                for dep in dependencies:
                    artifact_id = dep.find("./ns:artifactId", ns)
                    if artifact_id is not None:
                        artifact = artifact_id.text
                        if artifact and "spring-boot-starter-web" in artifact:
                            has_web = True
                        if artifact and ("spring-boot-starter-data" in artifact or "spring-data" in artifact):
                            has_data = True
                        if artifact and "spring-cloud" in artifact:
                            has_cloud = True
                        if artifact and "spring-security" in artifact:
                            has_security = True
                        if artifact and "spring-webflux" in artifact:
                            has_reactive = True
            except Exception:
                pass
                
        # Check Gradle build.gradle
        gradle_path = os.path.join(self.root_dir, "build.gradle")
        if os.path.exists(gradle_path):
            try:
                with open(gradle_path, 'r') as f:
                    content = f.read()
                    if "spring-boot-starter-web" in content:
                        has_web = True
                    if "spring-boot-starter-data" in content or "spring-data" in content:
                        has_data = True
                    if "spring-cloud" in content:
                        has_cloud = True
                    if "spring-security" in content:
                        has_security = True
                    if "spring-webflux" in content:
                        has_reactive = True
            except Exception:
                pass
                
        # Determine project type based on components
        if has_cloud:
            if has_web:
                self.project_type = "Microservice"
            else:
                self.project_type = "Spring Cloud Application"
        elif has_reactive:
            self.project_type = "Reactive Web Application"
        elif has_web:
            if has_data:
                self.project_type = "Web Application with Data Access"
            else:
                self.project_type = "Web Application"
        elif has_data:
            self.project_type = "Data Access Application"
        else:
            self.project_type = "Spring Boot Application"
            
        if has_security:
            self.project_type += " (with Security)"

    def _scan_directory(self, dir_path: str, depth: int = 0) -> Dict:
        """
        Recursively scan a directory and build the tree structure.
        
        Args:
            dir_path: Path to the directory
            depth: Current depth of recursion
            
        Returns:
            Dictionary representing the directory structure
        """
        # Check max depth
        if self.max_depth >= 0 and depth > self.max_depth:
            return {"name": os.path.basename(dir_path), "type": "dir", "children": [{"name": "...", "type": "max_depth_reached"}]}
            
        result = {"name": os.path.basename(dir_path), "type": "dir", "children": []}
        
        try:
            items = []
            for item in os.listdir(dir_path):
                full_path = os.path.join(dir_path, item)
                is_dir = os.path.isdir(full_path)
                
                if self._is_excluded(full_path, is_dir):
                    continue
                    
                items.append((full_path, is_dir))
            
            # Sort items: directories first, then files
            items.sort(key=lambda x: (not x[1], x[0].lower()))
            
            for full_path, is_dir in items:
                if is_dir:
                    child = self._scan_directory(full_path, depth + 1)
                    if child["children"]:  # Only add directories with content
                        result["children"].append(child)
                else:
                    file_type = self._detect_file_type(full_path)
                    result["children"].append({
                        "name": os.path.basename(full_path),
                        "type": file_type
                    })
                    
        except Exception as e:
            print(f"Error scanning directory {dir_path}: {e}", file=sys.stderr)
            result["children"].append({"name": "Error: " + str(e), "type": "error"})
            
        return result

    def analyze(self):
        """Analyze the Spring Boot project and build the project tree"""
        print(f"Analyzing Spring Boot project in {self.root_dir}...")
        
        # Detect JDK and Spring Boot versions
        self._detect_jdk_version()
        self._detect_spring_boot_version()
        
        # Scan the directory structure
        self.project_tree = self._scan_directory(self.root_dir)
        
        # Detect project type based on collected stats
        self._detect_project_type()
        
        print("Analysis complete.")

    def _format_text_tree(self, node: Dict, prefix: str = "", is_last: bool = True) -> str:
        """
        Format a tree node as text with appropriate indentation and styling.
        
        Args:
            node: Tree node to format
            prefix: Prefix for indentation
            is_last: Whether this is the last child of its parent
            
        Returns:
            Formatted tree as text
        """
        result = []
        
        # Determine the connector symbol and next prefix
        connector = "└── " if is_last else "├── "
        next_prefix = prefix + ("    " if is_last else "│   ")
        
        # Get node name with appropriate color
        node_type = node.get("type", "unknown")
        node_name = node.get("name", "")
        
        if self.use_color:
            if node_type == "dir":
                node_str = f"{Colors.BOLD}{Colors.BLUE}{node_name}/{Colors.RESET}"
            elif node_type == "controller":
                node_str = f"{Colors.GREEN}{node_name} [Controller]{Colors.RESET}"
            elif node_type == "service":
                node_str = f"{Colors.YELLOW}{node_name} [Service]{Colors.RESET}"
            elif node_type == "repository":
                node_str = f"{Colors.MAGENTA}{node_name} [Repository]{Colors.RESET}"
            elif node_type == "entity":
                node_str = f"{Colors.CYAN}{node_name} [Entity]{Colors.RESET}"
            elif node_type == "config":
                node_str = f"{Colors.RED}{node_name} [Configuration]{Colors.RESET}"
            elif node_type == "component":
                node_str = f"{Colors.YELLOW}{node_name} [Component]{Colors.RESET}"
            elif node_type == "resource":
                node_str = f"{Colors.BOLD}{Colors.CYAN}{node_name} [Resource]{Colors.RESET}"
            elif node_type == "error":
                node_str = f"{Colors.RED}{node_name}{Colors.RESET}"
            elif node_type == "max_depth_reached":
                node_str = f"{Colors.YELLOW}{node_name}{Colors.RESET}"
            else:
                node_str = node_name
        else:
            type_label = ""
            if node_type == "dir":
                node_str = f"{node_name}/"
            elif node_type in ["controller", "service", "repository", "entity", "config", "component", "resource"]:
                type_label = f" [{node_type.capitalize()}]"
                node_str = f"{node_name}{type_label}"
            else:
                node_str = node_name
                
        result.append(f"{prefix}{connector}{node_str}")
        
        # Process children if any
        children = node.get("children", [])
        for i, child in enumerate(children):
            is_last_child = i == len(children) - 1
            result.append(self._format_text_tree(child, next_prefix, is_last_child))
            
        return "\n".join(result)

    def _format_summary(self) -> str:
        """
        Format project summary information.
        
        Returns:
            Formatted summary as text
        """
        separator = "=" * 50
        
        if self.use_color:
            summary = [
                f"\n{Colors.BOLD}{separator}{Colors.RESET}",
                f"{Colors.BOLD}{Colors.GREEN}Spring Boot Project Summary{Colors.RESET}",
                f"{Colors.BOLD}{separator}{Colors.RESET}",
                f"{Colors.BOLD}JDK Version:{Colors.RESET} {self.jdk_version}",
                f"{Colors.BOLD}Spring Boot Version:{Colors.RESET} {self.spring_boot_version}",
                f"{Colors.BOLD}Project Type:{Colors.RESET} {self.project_type}",
                f"{Colors.BOLD}{separator}{Colors.RESET}",
                f"{Colors.BOLD}Project Statistics:{Colors.RESET}",
                f"  {Colors.GREEN}Controllers:{Colors.RESET} {self.stats['controller']}",
                f"  {Colors.YELLOW}Services:{Colors.RESET} {self.stats['service']}",
                f"  {Colors.MAGENTA}Repositories:{Colors.RESET} {self.stats['repository']}",
                f"  {Colors.CYAN}Entities:{Colors.RESET} {self.stats['entity']}",
                f"  {Colors.RED}Configurations:{Colors.RESET} {self.stats['config']}",
                f"  {Colors.YELLOW}Components:{Colors.RESET} {self.stats['component']}",
                f"  {Colors.CYAN}Resources:{Colors.RESET} {self.stats['resources']}",
                f"{Colors.BOLD}{separator}{Colors.RESET}",
            ]
        else:
            summary = [
                f"\n{separator}",
                "Spring Boot Project Summary",
                separator,
                f"JDK Version: {self.jdk_version}",
                f"Spring Boot Version: {self.spring_boot_version}",
                f"Project Type: {self.project_type}",
                separator,
                "Project Statistics:",
                f"  Controllers: {self.stats['controller']}",
                f"  Services: {self.stats['service']}",
                f"  Repositories: {self.stats['repository']}",
                f"  Entities: {self.stats['entity']}",
                f"  Configurations: {self.stats['config']}",
                f"  Components: {self.stats['component']}",
                f"  Resources: {self.stats['resources']}",
                separator,
            ]
            
        return "\n".join(summary)

    def display_tree(self):
        """Display the project tree structure"""
        if not self.project_tree:
            print("No project structure to display. Please run analyze() first.")
            return
            
        print(self._format_text_tree(self.project_tree))
        print(self._format_summary())

    def to_json(self) -> str:
        """
        Convert the project tree to JSON.
        
        Returns:
            JSON string representation of the project tree
        """
        result = {
            "project_tree": self.project_tree,
            "summary": {
                "jdk_version": self.jdk_version,
                "spring_boot_version": self.spring_boot_version,
                "project_type": self.project_type,
                "statistics": dict(self.stats)
            }
        }
        
        return json.dumps(result, indent=2)


def parse_arguments():
    """Parse command line arguments"""
    parser = argparse.ArgumentParser(description="Spring Boot Project Analyzer")
    
    parser.add_argument("directory", nargs="?", default=".",
                      help="Directory to analyze (default: current directory)")
    
    parser.add_argument("--exclude-file", action="append", dest="exclude_files",
                      help="Exclude specific file by name (can be used multiple times)")
    
    parser.add_argument("--exclude-ext", action="append", dest="exclude_exts",
                      help="Exclude files by extension without dot (can be used multiple times)")
    
    parser.add_argument("--exclude-dir", action="append", dest="exclude_dirs",
                      help="Exclude directory (can be used multiple times)")
    
    parser.add_argument("--exclude-pattern", action="append", dest="exclude_patterns",
                      help="Exclude by pattern using glob syntax (can be used multiple times)")
    
    parser.add_argument("--output", choices=["text", "json"], default="text",
                      help="Output format (text or json)")
    
    parser.add_argument("--color", type=lambda x: x.lower() == "true", default=True,
                      help="Enable/disable color output (true/false)")
    
    parser.add_argument("--max-depth", type=int, default=-1,
                      help="Maximum depth to scan (-1 for unlimited)")
    
    # Note: We don't need to add --help/-h as it's automatically added by argparse
    
    args = parser.parse_args()
    return args


def main():
    """Main function"""
    args = parse_arguments()
    
    try:
        analyzer = SpringBootAnalyzer(
            root_dir=args.directory,
            exclude_files=args.exclude_files,
            exclude_exts=args.exclude_exts,
            exclude_dirs=args.exclude_dirs,
            exclude_patterns=args.exclude_patterns,
            max_depth=args.max_depth,
            use_color=args.color
        )
        
        analyzer.analyze()
        
        if args.output == "json":
            print(analyzer.to_json())
        else:
            analyzer.display_tree()
            
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
