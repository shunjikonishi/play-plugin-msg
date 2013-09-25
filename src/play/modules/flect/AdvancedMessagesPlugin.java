package play.modules.flect;

import java.io.File;
import java.io.IOException;
import play.Play;
import play.PlayPlugin;
import play.Logger;
import play.vfs.VirtualFile;

public class AdvancedMessagesPlugin extends PlayPlugin {
	
	private static final String MESSAGES        = "conf/messages";
	private static final String MESSAGES_ORIGIN = "conf/messages.origin";
	
	@Override
	public void onApplicationStart() {
		VirtualFile msgFile = Play.getVirtualFile(MESSAGES);
		VirtualFile originFile = Play.getVirtualFile(MESSAGES_ORIGIN);
		if (msgFile == null || originFile == null) {
			return;
		}
		if (msgFile.lastModified() > originFile.lastModified()) {
			return;
		}
		File javaFile = null;
		String javaPackage = null;
		String java = Play.configuration.getProperty("flect.messages.java");
		if (java != null) {
			javaFile = new File("app", java.replace(".", "/") + ".java");
			int idx = java.lastIndexOf('.');
			javaPackage = idx == -1 ? null : java.substring(0, idx);
		}
		ResourceGen gen = new ResourceGen(
			originFile.getRealFile().getParentFile(), msgFile.getName(), 
			javaFile, javaPackage, "ja");
		try {
			gen.process(originFile.getRealFile());
			Logger.info("AdvancedMessagesPlugin - Regenerate messages");
		} catch (IOException e) {
            Logger.warn("AdvancedMessagesPlugin - Failure resource generate: " + e.toString());
		}
	}
	
	@Override
    public boolean detectClassesChange() {
		VirtualFile msgFile    = Play.getVirtualFile(MESSAGES);
		VirtualFile originFile = Play.getVirtualFile(MESSAGES_ORIGIN);
		if (msgFile == null || originFile == null) {
			return false;
		}
		if (msgFile.lastModified() < originFile.lastModified()) {
			onApplicationStart();
			return true;
		}
		return false;
	}
	
}
