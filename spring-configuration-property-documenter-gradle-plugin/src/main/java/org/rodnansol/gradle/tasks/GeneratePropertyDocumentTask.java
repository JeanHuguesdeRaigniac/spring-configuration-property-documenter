package org.rodnansol.gradle.tasks;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.rodnansol.core.action.DocumentGenerationAction;
import org.rodnansol.core.generator.template.HandlebarsTemplateCompiler;
import org.rodnansol.core.generator.template.TemplateType;
import org.rodnansol.core.generator.template.customization.AsciiDocTemplateCustomization;
import org.rodnansol.core.generator.template.customization.HtmlTemplateCustomization;
import org.rodnansol.core.generator.template.customization.MarkdownTemplateCustomization;
import org.rodnansol.core.generator.template.customization.TemplateCustomization;
import org.rodnansol.core.generator.template.customization.XmlTemplateCustomization;
import org.rodnansol.core.project.ProjectFactory;
import org.rodnansol.core.project.gradle.GradleProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Task to generate a single document.
 *
 * @author nandorholozsnyak
 * @author tkhadiradeo
 * @since 0.5.0
 */
public abstract class GeneratePropertyDocumentTask extends ConventionTask {

    /**
     * Name that should be generated to the final document.
     *
     * @since 0.5.0
     */
    @Input
    @Option(option = "documentName", description = "Name/header of the generated document")
    private String documentName;
    /**
     * Description about the project.
     *
     * @since 0.5.0
     */
    @Input
    @Optional
    @Option(option = "documentDescription", description = "Description")
    private String documentDescription;
    /**
     * The template to be used.
     *
     * @since 0.5.0
     */
    @Input
    @Optional
    @Option(option = "template", description = "Template file's path")
    private String template;
    /**
     * Type of the document.
     * <p>
     * The following template types are supported:
     * <ul>
     *     <li>MARKDOWN</li>
     *     <li>ADOC</li>
     *     <li>HTML</li>
     *     <li>XML</li>
     * </ul>
     *
     * @since 0.5.0
     */
    @Input
    @Option(option = "type", description = "Type of the final rendered document")
    private String type;
    /**
     * Metadata input that can be:
     * <ul>
     *     <li>A path to JSON file for example: <b>target/classes/META-INF/spring-configuration-metadata.json</b></li>
     *     <li>A directory that contains the file</li>
     *     <li>A jar/zip file that contains the file within the following entry <b>META-INF/spring-configuration-metadata.json</b></li>
     * </ul>
     *
     * @since 0.5.0
     */
    @InputFile
    @Optional
    @Option(option = "metadataInput", description = "Metadata input file")
    private File metadataInput = new File("build/classes/java/main/META-INF/spring-configuration-metadata.json");
    /**
     * Output file.
     *
     * @since 0.5.0
     */
    @OutputFile
    @Optional
    @Option(option = "outputFile", description = "Output file")
    private File outputFile;
    /**
     * Define if the process should fail on an error or not.
     *
     * @since 0.5.0
     */
    @Input
    @Optional
    @Option(option = "failOnError", description = "Fail on error or not")
    private boolean failOnError = false;
    /**
     * Template compiler class's fully qualified name .
     * <p>
     * With this option you can use your own template compiler implementation if the default {@link HandlebarsTemplateCompiler}. based one is not enough.
     *
     * @since 0.5.0
     */
    @Input
    @Optional
    @Option(option = "templateCompilerName", description = "Name of the template compiler, by default Handlebars will be used")
    private String templateCompilerName = HandlebarsTemplateCompiler.class.getName();
    /**
     * List of excluded properties.
     *
     * @since 0.5.0
     */
    @Input
    @Optional
    @Option(option = "excludedProperties", description = "List of excluded properties")
    private List<String> excludedProperties = new ArrayList<>();
    /**
     * List of included properties.
     *
     * @since 0.5.0
     */
    @Input
    @Optional
    @Option(option = "includedProperties", description = "List of included properties")
    private List<String> includedProperties = new ArrayList<>();
    /**
     * List of excluded groups.
     *
     * @since 0.5.0
     */
    @Input
    @Optional
    @Option(option = "excludedGroups", description = "List of excluded groups")
    private List<String> excludedGroups = new ArrayList<>();
    /**
     * List of included groups.
     *
     * @since 0.5.0
     */
    @Input
    @Optional
    @Option(option = "includedGroups", description = "List of included groups")
    private List<String> includedGroups = new ArrayList<>();

    /**
     * AsciiDoc template customization object to configure the template.
     *
     * @since 0.5.0
     */
    @Internal
    private AsciiDocTemplateCustomization asciiDocCustomization = new AsciiDocTemplateCustomization();

    /**
     * HTML template customization object to configure the template.
     *
     * @since 0.5.0
     */
    @Internal
    private HtmlTemplateCustomization htmlCustomization = new HtmlTemplateCustomization();

    /**
     * Markdown template customization object to configure the template.
     *
     * @since 0.5.0
     */
    @Internal
    private MarkdownTemplateCustomization markdownCustomization = new MarkdownTemplateCustomization();

    /**
     * XML template customization object to configure the template.
     *
     * @since 0.5.0
     */
    @Internal
    private XmlTemplateCustomization xmlCustomization = new XmlTemplateCustomization();

    @TaskAction
    public void execute() {
        try {
            getLogger().info("Generating document based on task configuration:[{}]", this);
            DocumentGenerationAction documentGenerationAction = setupAction();
            documentGenerationAction.execute();
        } catch (Exception e) {
            if (failOnError) {
                throw new RuntimeException(e);
            } else {
                getLogger().error("Error during file generation, failOnError is set to false, check the logs: {} ", e);
            }
        }
    }

    private DocumentGenerationAction setupAction() {
        Project project = getProject();
        GradleProject gradleProject = ProjectFactory.ofGradleProject(project.getProjectDir(), project.getName(), List.of());
        DocumentGenerationAction documentGenerationAction = new DocumentGenerationAction(gradleProject, documentName, getActualTemplateCustomization(), TemplateType.valueOf(type), metadataInput);
        documentGenerationAction.setTemplateCompilerName(templateCompilerName);
        documentGenerationAction.setDescription(documentDescription);
        documentGenerationAction.setTemplate(template);
        documentGenerationAction.setOutputFile(outputFile);
        documentGenerationAction.setExcludedGroups(excludedGroups);
        documentGenerationAction.setIncludedGroups(includedGroups);
        documentGenerationAction.setExcludedProperties(excludedProperties);
        documentGenerationAction.setIncludedProperties(includedProperties);
        return documentGenerationAction;
    }

    private TemplateCustomization getActualTemplateCustomization() {
        switch (TemplateType.valueOf(type)) {
            case MARKDOWN:
                return markdownCustomization;
            case ADOC:
                return asciiDocCustomization;
            case HTML:
                return htmlCustomization;
            case XML:
                return xmlCustomization;
        }
        throw new IllegalStateException("There is no template customization set for the current run");
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentDescription() {
        return documentDescription;
    }

    public void setDocumentDescription(String documentDescription) {
        this.documentDescription = documentDescription;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public File getMetadataInput() {
        return metadataInput;
    }

    public void setMetadataInput(File metadataInput) {
        this.metadataInput = metadataInput;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public Boolean getFailOnError() {
        return failOnError;
    }

    public void setFailOnError(Boolean failOnError) {
        this.failOnError = failOnError;
    }

    public String getTemplateCompilerName() {
        return templateCompilerName;
    }

    public void setTemplateCompilerName(String templateCompilerName) {
        this.templateCompilerName = templateCompilerName;
    }

    public List<String> getExcludedProperties() {
        return excludedProperties;
    }

    public void setExcludedProperties(List<String> excludedProperties) {
        this.excludedProperties = excludedProperties;
    }

    public List<String> getIncludedProperties() {
        return includedProperties;
    }

    public void setIncludedProperties(List<String> includedProperties) {
        this.includedProperties = includedProperties;
    }

    public List<String> getExcludedGroups() {
        return excludedGroups;
    }

    public void setExcludedGroups(List<String> excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public List<String> getIncludedGroups() {
        return includedGroups;
    }

    public void setIncludedGroups(List<String> includedGroups) {
        this.includedGroups = includedGroups;
    }

    public AsciiDocTemplateCustomization getAsciiDocCustomization() {
        return asciiDocCustomization;
    }

    public HtmlTemplateCustomization getHtmlCustomization() {
        return htmlCustomization;
    }

    public MarkdownTemplateCustomization getMarkdownCustomization() {
        return markdownCustomization;
    }

    public XmlTemplateCustomization getXmlCustomization() {
        return xmlCustomization;
    }

    @Override
    public String toString() {
        return "GenerateDocumentTask{" +
            "documentName='" + documentName + '\'' +
            ", documentDescription='" + documentDescription + '\'' +
            ", template='" + template + '\'' +
            ", type='" + type + '\'' +
            ", metadataInput=" + metadataInput +
            ", outputFile=" + outputFile +
            ", failOnError=" + failOnError +
            ", templateCompilerName='" + templateCompilerName + '\'' +
            ", excludedProperties=" + excludedProperties +
            ", includedProperties=" + includedProperties +
            ", excludedGroups=" + excludedGroups +
            ", includedGroups=" + includedGroups +
            ", asciiDocCustomization=" + asciiDocCustomization +
            ", htmlCustomization=" + htmlCustomization +
            ", markdownCustomization=" + markdownCustomization +
            ", xmlCustomization=" + xmlCustomization +
            "} " + super.toString();
    }

    /**
     * DSL entry point for the {@link GeneratePropertyDocumentTask#markdownCustomization} field.
     */
    public void markdownCustomization(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.setDelegate(markdownCustomization);
        closure.call();
    }

    /**
     * DSL entry point for the {@link GeneratePropertyDocumentTask#asciiDocCustomization} field.
     */
    public void asciiDocCustomization(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.setDelegate(asciiDocCustomization);
        closure.call();
    }

    /**
     * DSL entry point for the {@link GeneratePropertyDocumentTask#htmlCustomization} field.
     */
    public void htmlCustomization(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.setDelegate(htmlCustomization);
        closure.call();
    }

    /**
     * DSL entry point for the {@link GeneratePropertyDocumentTask#xmlCustomization} field.
     */
    public void xmlCustomization(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.setDelegate(xmlCustomization);
        closure.call();
    }

}
