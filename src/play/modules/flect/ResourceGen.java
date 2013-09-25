package play.modules.flect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ResourceGen {
	
	private File outputDir;
	private String basename;
	
	private File javaFile;
	private String javaPackage;
	private String commentLang;
	
	public ResourceGen(File outputDir, String basename, File javaFile, String javaPackage, String commentLang) {
		this.outputDir = outputDir;
		this.basename = basename;
		this.javaFile = javaFile;
		this.javaPackage = javaPackage;
		this.commentLang = commentLang;
	}
	
	public void process(File f) throws IOException {
		Context context = new Context();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
		try {
			String line = reader.readLine();
			while (line != null) {
				if (line.length() > 0 && line.charAt(0) != '#') {
					context.add(line);
				}
				line = reader.readLine();
			}
		} finally {
			reader.close();
		}
		checkContext(context);
		
		for (String lang : context.getLangList()) {
			String filename = basename;
			if (lang.length() > 0) {
				filename += "." + lang;
			}
			generateResource(new File(outputDir, filename), context.getMap(lang));
		}
		if (javaFile != null) {
			Map map = commentLang == null ? context.getBaseMap() : context.getMap(commentLang);
			if (map == null) {
				System.err.println("Resource not found: lang=" + commentLang);
			} else {
				generateJava(javaFile, map, context.getBaseMap());
			}
		}
	}
	
	private void checkContext(Context context) {
		Map<String, String> baseMap = context.getBaseMap();
		if (baseMap == null) {
			System.err.println("Default resource not found.");
			return;
		}
		List<String> langList = context.getLangList();
		int cnt = baseMap.size();
		for (String lang : langList) {
			if (lang.equals("")) {
				continue;
			}
			Map<String, String> map = context.getMap(lang);
			for (String key : map.keySet()) {
				if (baseMap.get(key) == null) {
					System.err.println("Default resource not defined: lang=" + lang + ", key=" + key);
				}
			}
		}
	}
	
	private void generateResource(File f, Map<String, String> map) throws IOException {
		StringBuilder buf = new StringBuilder();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			buf.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
		}
		writeFile(f, buf.toString().getBytes("utf-8"));
	}
	
	private void generateJava(File f, Map<String, String> map, Map<String, String> baseMap) throws IOException {
		String name = f.getName();
		int idx = name.indexOf('.');
		if (idx != -1) {
			name = name.substring(0, idx);
		} else {
			f = new File(f.getParentFile(), name + ".java");
		}
		StringBuilder buf = new StringBuilder();
		if (this.javaPackage != null) {
			buf.append("package ").append(this.javaPackage).append(";\n\n");
		}
		buf.append("public class ").append(name).append(" {\n\n");
		for (Map.Entry<String, String> entry : baseMap.entrySet()) {
			String key = entry.getKey();
			String value = map.get(key);
			if (value == null) {
				value = entry.getValue();
			}
			buf.append("\t/** ").append(value)
				.append(" */\n\tpublic static final String ").append(getSymbol(key))
				.append(" = \"").append(key).append("\";\n\n");
		}
		buf.append("}\n");
		writeFile(f, buf.toString().getBytes("utf-8"));
	}
	
	private String getSymbol(String s) {
		StringBuilder buf = new StringBuilder();
		boolean prevIsLower = false;
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c == '.') {
				buf.append('_');
				prevIsLower = false;
			} else if (c >= 'A' && c <= 'Z') {
				if (prevIsLower) {
					buf.append('_');
				}
				buf.append(c);
				prevIsLower = false;
			} else if (c >= 'a' && c <= 'z') {
				buf.append(Character.toUpperCase(c));
				prevIsLower = true;
			} else {
				buf.append(c);
				prevIsLower = false;
			}
		}
		return buf.toString();
	}
	
	private static class Context {
		
		private Map<String, Map<String, String>> langMap = new HashMap<String, Map<String, String>>();
		
		public void add(String s) {
			int idx = s.indexOf('=');
			if (idx == -1) {
				return;
			}
			String key = s.substring(0, idx);
			String value = s.substring(idx+1);
			String lang = "";
			if (key.endsWith("]")) {
				idx = key.indexOf('[');
				if (idx != -1) {
					lang = key.substring(idx+1, key.length() - 1);
					key = key.substring(0, idx);
				}
			}
			Map<String, String> map = langMap.get(lang);
			if (map == null) {
				map = new TreeMap<String, String>();
				langMap.put(lang, map);
			}
			map.put(key, value);
		}
		
		public Map<String, String> getBaseMap() {
			return langMap.get("");
		}
		
		public List<String> getLangList() {
			return new ArrayList<String>(langMap.keySet());
		}
		
		public Map<String, String> getMap(String lang) {
			return langMap.get(lang);
		}
	}
	
	private void writeFile(File file, byte[] data) throws IOException {
		FileOutputStream os = new FileOutputStream(file);
		try {
			os.write(data);
		} finally {
			os.close();
		}
	}
	
	private static void printUsage() {
		System.err.println("resourcegen [-outputDir OUTDIR] [-outputFile filename] " + 
			"[-javaFile FILENAME] [-javaPackage PACKAGE] [-comment LANG] filename");
		System.exit(-1);
	}
	
	public static void main(String[] args) throws IOException {
		String outputDir = null;
		String basename = null;
		String javaFile = null;
		String javaPackage = null;
		String commentLang = null;
		String filename = null;
		try {
			for (int i=0; i<args.length; i++) {
				String s = args[i];
				if ("-outputDir".equals(s)) {
					outputDir = args[++i];
				} else if ("-outputFile".equals(s)) {
					basename = args[++i];
				} else if ("-javaFile".equals(s)) {
					javaFile = args[++i];
				} else if ("-javaPackage".equals(s)) {
					javaPackage = args[++i];
				} else if ("-comment".equals(s)) {
					commentLang = args[++i];
				} else if (filename == null) {
					filename = s;
				} else {
					printUsage();
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			printUsage();
		}
		if (filename == null) {
			printUsage();
		}
		if (javaFile != null && javaPackage == null) {
			System.err.println("Specify javaPackage, when javaFile is specified.");
			printUsage();
		}
		
		
		File file = new File(filename);
		if (basename == null) {
			int idx = file.getName().indexOf('.');
			if (idx == -1) {
				if (outputDir == null) {
					System.err.println("Set -outputDir or -outputFile");
					printUsage();
				}
				basename = file.getName();
			} else {
				basename = file.getName().substring(0, idx);
			}
		}
		File dir = outputDir == null ? file.getParentFile() : new File(outputDir);
		File jf = javaFile == null ? null : new File(javaFile);
		ResourceGen gen = new ResourceGen(dir, basename, jf, javaPackage, commentLang);
		gen.process(file);
	}
}
