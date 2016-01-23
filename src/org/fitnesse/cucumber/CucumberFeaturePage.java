package org.fitnesse.cucumber;

import fitnesse.wiki.*;
import fitnesse.wikitext.parser.*;
import org.apache.commons.lang.NotImplementedException;
import util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

public class CucumberFeaturePage extends BaseWikiPage {
    private final File path;
    private final VariableSource variableSource;
    private String content;
    private ParsingPage parsingPage;
    private Symbol syntaxTree;

    public CucumberFeaturePage(File path, String name, WikiPage parent, VariableSource variableSource) {
        super(name, parent);
        this.variableSource = variableSource;
        this.path = path;
    }

    @Override
    public WikiPage addChildPage(String name) {
        throw new NotImplementedException("Can not add child pages to Cucumber feature pages");
    }

    @Override
    public boolean hasChildPage(String name) {
        return false;
    }

    @Override
    public WikiPage getChildPage(String name) {
        return null;
    }

    @Override
    public void removeChildPage(String name) {
        // no-op
    }

    @Override
    public List<WikiPage> getChildren() {
        return emptyList();
    }

    @Override
    public PageData getData() {
        return new PageData(readContent(), wikiPageProperties());
    }

    private WikiPageProperties wikiPageProperties() {
        WikiPageProperties properties = new WikiPageProperties();
        properties.set(PageType.TEST.toString());
        return properties;
    }

    private ParsingPage getParsingPage() {
        parse();
        return parsingPage;
    }

    private Symbol getSyntaxTree() {
        parse();
        return syntaxTree;
    }

    private void parse() {
        if (syntaxTree == null) {
            // This is the only page where we need a VariableSource
            parsingPage = makeParsingPage();
            syntaxTree = Parser.make(parsingPage, enrichWithWikiMarkup(getData().getContent())).parse();
        }
    }

    public ParsingPage makeParsingPage() {
        ParsingPage.Cache cache = new ParsingPage.Cache();

        VariableSource compositeVariableSource = new CompositeVariableSource(
                new ApplicationVariableSource(variableSource),
                new PageVariableSource(this),
                new BaseWikitextPage.UserVariableSource(variableSource),
                cache,
                new BaseWikitextPage.ParentPageVariableSource(this),
                variableSource);
        return new ParsingPage(new WikiSourcePage(this), compositeVariableSource, cache);
    }

    private String enrichWithWikiMarkup(String content) {
        return content.replaceAll("(?im)^scenario:", "!3 Scenario:");
    }

    @Override
    public Collection<VersionInfo> getVersions() {
        return emptyList();
    }

    @Override
    public WikiPage getVersion(String versionName) {
        return this;
    }

    @Override
    public String getHtml() {
        return new HtmlTranslator(new WikiSourcePage(this), getParsingPage()).translateTree(getSyntaxTree());
    }

    private String readContent() {
        if (content == null) {
            try {
                content = FileUtil.getFileContent(path);
            } catch (IOException e) {
                content = String.format("<p class='error'>Unable to read feature file %s: %s</p>", path, e.getMessage());
            }
        }
        return content;
    }

    @Override
    public VersionInfo commit(PageData data) {
        return null;
    }

    @Override
    public String getVariable(String name) {
        ParsingPage parsingPage = getParsingPage();
        Maybe<String> variable = parsingPage.findVariable(name);
        if (variable.isNothing()) return null;

        Parser parser = Parser.make(parsingPage, "", SymbolProvider.variableDefinitionSymbolProvider);
        return new HtmlTranslator(null, parsingPage).translate(parser.parseWithParent(variable.getValue(), null));
    }

    public File getFileSystemPath() {
        return path;
    }
}
