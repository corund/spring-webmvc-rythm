package com.ctlok.springframework.web.servlet.view.rythm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.rythmengine.Rythm;
import org.rythmengine.template.ITemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

import com.ctlok.springframework.web.servlet.view.rythm.tag.CookieValue;
import com.ctlok.springframework.web.servlet.view.rythm.tag.CsrfToken;
import com.ctlok.springframework.web.servlet.view.rythm.tag.DateFormat;
import com.ctlok.springframework.web.servlet.view.rythm.tag.FileBasedTag;
import com.ctlok.springframework.web.servlet.view.rythm.tag.FileBasedTagProxy;
import com.ctlok.springframework.web.servlet.view.rythm.tag.FullUrl;
import com.ctlok.springframework.web.servlet.view.rythm.tag.Message;
import com.ctlok.springframework.web.servlet.view.rythm.tag.Secured;
import com.ctlok.springframework.web.servlet.view.rythm.tag.Url;
import com.ctlok.springframework.web.servlet.view.rythm.variable.HttpServletRequestVariable;
import com.ctlok.springframework.web.servlet.view.rythm.variable.ImplicitVariable;

/**
 * @author Lawrence Cheung
 *
 */
public class RythmViewResolver extends AbstractTemplateViewResolver {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RythmViewResolver.class);
    
    protected static final String BUILD_IN_FILE_BASED_TAG_LOCATION = 
            "classpath:com/ctlok/springframework/web/servlet/view/rythm/tag/template/";
    
    private final RythmConfigurator configurator;
    
    public RythmViewResolver(final RythmConfigurator configurator) {
        super();
        this.configurator = configurator;
        this.setViewClass(this.requiredViewClass());
        this.setContentType("text/html");
    }

	@Override
    protected Class<?> requiredViewClass() {
        return RythmView.class;
    }

    @Override
    protected void initServletContext(ServletContext servletContext) {
        super.initServletContext(servletContext);
        this.setupSpringRythmConfig();
        this.initRythm();
    }
    
    protected void initRythm(){
        
        final Map<String, Object> config = configurator.generateConfig();
        
        LOGGER.debug("Rythm config: [{}]", config);
        
        Rythm.init(config);
        
        if (configurator.getTags() != null){
        	for (final ITemplate tag: configurator.getTags()){
        	    
        		LOGGER.debug("Register tag: [{}]", tag.__getName());
        		
                Rythm.engine().registerTemplate(tag);
                
        	}
        }
        
        if (configurator.getFileBasedTags() != null){
        
            try{
                
                boolean disableFileWrite = Rythm.engine().conf().disableFileWrite();
                String fileBasedTagTempDirectory = configurator.getFileBasedTagTempDirectory();
                
                if (fileBasedTagTempDirectory == null){
                    
                    fileBasedTagTempDirectory = Helper.TEMP_DIR;
                    
                }
                
                for (final FileBasedTag fileBasedTag: configurator.getFileBasedTags()){
        
                    final ITemplate tag = new FileBasedTagProxy(
                            fileBasedTag, fileBasedTagTempDirectory, disableFileWrite);
    
                    LOGGER.debug("Register file based tag: [{}]", tag.__getName());
    
                    Rythm.engine().registerTemplate(tag);
        
                }
            
            } catch (IOException e){
                
                throw new IllegalStateException(e);
                
            }
        
        }
        
        LOGGER.info("Rythm version [{}] setup success.", Rythm.engine().version());
        
    }
    
    protected void setupSpringRythmConfig(){
    	this.setupBuildInImplicitVariables();
    	this.setupBuildInImplicitPackage();
        this.setupBuiltInTag();
        this.setupBuiltInFileBasedTag();
    }
    
    protected void setupBuildInImplicitVariables(){
        
    	if (configurator.getImplicitVariables() == null){
    	    
    		configurator.setImplicitVariables(new ArrayList<ImplicitVariable>());
    		
    	}
        			
    	configurator.getImplicitVariables().add(new HttpServletRequestVariable());
    }
    
    protected void setupBuildInImplicitPackage(){
        
        if (configurator.getImplicitPackages() == null){
            
            configurator.setImplicitPackages(new ArrayList<String>());
            
        }
        
        configurator.getImplicitPackages().add(
                com.ctlok.springframework.web.servlet.view.rythm.constant.DefaultApplicationVariable.class.getName());
        
        configurator.getImplicitPackages().add(
                com.ctlok.springframework.web.servlet.view.rythm.constant.DefaultRequestParameterName.class.getName());
        
        configurator.getImplicitPackages().add(
                com.ctlok.springframework.web.servlet.view.rythm.constant.DefaultSessionAttributeName.class.getName());
        
        configurator.getImplicitPackages().add(
                com.ctlok.springframework.web.servlet.view.rythm.form.Form.class.getName());
        
        configurator.getImplicitPackages().add(
                com.ctlok.springframework.web.servlet.view.rythm.tag.helper.SpringRythmFormHelper.class.getName());
        
    }
    
    protected void setupBuiltInTag(){
        
    	if (configurator.getTags() == null){
    	    
    		configurator.setTags(new ArrayList<ITemplate>());
    		
    	}
    	
    	for (final ITemplate tag: this.createBuiltInTags()){
    	    
    		configurator.getTags().add(tag);
    		
    	}

    }
    
    protected void setupBuiltInFileBasedTag(){
        
        if (configurator.getFileBasedTags() == null){
            
            configurator.setFileBasedTags(new ArrayList<FileBasedTag>());
            
        }
        
        for (final FileBasedTag fileBasedTag: createBuiltInFileBasedTags()){
            
            configurator.getFileBasedTags().add(fileBasedTag);
            
        }
        
    }
 
    protected List<ITemplate> createBuiltInTags(){
        
        final List<ITemplate> tags = new ArrayList<ITemplate>();
        
        tags.add(new Url());
        tags.add(new FullUrl());
        tags.add(new Message(getApplicationContext()));
        tags.add(new Secured());
        tags.add(new DateFormat());
        tags.add(new CookieValue());
        tags.add(new CsrfToken(configurator.getCsrfTokenGenerator(), 
                configurator.getCsrfTokenSessionName()));
        
        return tags;
        
    }
    
    protected List<FileBasedTag> createBuiltInFileBasedTags(){
        
        final List<FileBasedTag> fileBasedTags = new ArrayList<FileBasedTag>();
        
        fileBasedTags.add(createFileBasedTag("hiddenCsrfToken.html", "hiddenCsrfToken"));
        
        fileBasedTags.add(createFileBasedTag("htmlForm.html", "htmlForm"));
        
        fileBasedTags.add(createFileBasedTag("inputHidden.html", "inputHidden"));
        
        fileBasedTags.add(createFileBasedTag("inputCheckbox.html", "inputCheckbox"));
        
        fileBasedTags.add(createFileBasedTag("inputOption.html", "inputOption"));
        
        fileBasedTags.add(createFileBasedTag("inputRadio.html", "inputRadio"));
        
        fileBasedTags.add(createFileBasedTag("inputSelect.html", "inputSelect"));
        
        fileBasedTags.add(createFileBasedTag("inputText.html", "inputText"));
        
        fileBasedTags.add(createFileBasedTag("inputTextarea.html", "inputTextarea"));
        
        return fileBasedTags;
        
    }
    
    protected Resource createTagResource(final String fileName){
        
        return getApplicationContext().getResource(BUILD_IN_FILE_BASED_TAG_LOCATION + fileName);
        
    }
	
	protected List<File> findTemplateFile(final File root){
		final List<File> templateFiles = new ArrayList<File>();
		
		if (root.isDirectory()){
			for (final File file: root.listFiles()){
				
				if (file.isFile()){
					templateFiles.add(file);
				} else {
					templateFiles.addAll(findTemplateFile(file));
				}
				
			}
		}
		
		return templateFiles;
	}
	
	protected FileBasedTag createFileBasedTag(String templateName, String tagName){
	    
	    return new FileBasedTag(createTagResource(templateName), tagName);
	    
	}

}
