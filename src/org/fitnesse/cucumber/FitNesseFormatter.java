package org.fitnesse.cucumber;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import fitnesse.testsystems.ExecutionResult;
import fitnesse.testsystems.TestSummary;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;

import static fitnesse.html.HtmlUtil.escapeHTML;
import static java.lang.String.format;

class FitNesseFormatter implements Formatter, Reporter {

    private final Printer outputPrinter;
    private final Printer errorPrinter;

    private Queue<Step> currentSteps = new ArrayDeque<>();
    private List<String> exampleHeaders;
    private Queue<ExamplesTableRow> examples = new ArrayDeque<>();

    private final TestSummary testSummary;
    private String examplesKeyword;


    public FitNesseFormatter(final TestSummary testSummary, Printer outputPrinter, Printer errorPrinter) {
        this.outputPrinter = outputPrinter;
        this.errorPrinter = errorPrinter;
        this.testSummary = testSummary;
    }

    @Override
    public void uri(final String uri) {
    }

    @Override
    public void background(final Background background) {
        write("h4", background);
    }

    @Override
    public void syntaxError(final String state, final String event, final List<String> legalEvents, final String uri, final Integer line) {
        write("syntaxError " + state + " " + event + "<br/>");
    }

    @Override
    public void feature(final Feature feature) {
        write("h3", feature);
    }

    @Override
    public void scenarioOutline(final ScenarioOutline scenarioOutline) {
        write("h4", scenarioOutline);
    }

    @Override
    public void scenario(final Scenario scenario) {
        if (examples.isEmpty()) {
            write("h4", scenario);
        } else {
            final ExamplesTableRow values = examples.poll();
            write("<h5>" + examplesKeyword + ": ");
            for (int i = 0; i < exampleHeaders.size(); i++) {
                if (i > 0) write(", ");
                write(exampleHeaders.get(i) + " = " + values.getCells().get(i));
            }
            write("</h5>");
        }
    }

    private void write(String tag, DescribedStatement statement) {
        write("<" + tag + ">" + statement.getKeyword()+ ": " + statement.getName() + "</" + tag + ">");
        if (statement.getDescription() != null) {
            write("<p style='white-space: pre-line'>" + statement.getDescription() + "</p>");
        }
    }

    @Override
    public void examples(final Examples examples) {
        examplesKeyword = examples.getKeyword();
        this.examples.addAll(examples.getRows());
        this.exampleHeaders = this.examples.poll().getCells();
    }

    @Override
    public void startOfScenarioLifeCycle(final Scenario scenario) {
        currentSteps.clear();
    }

    @Override
    public void step(final Step step) {
        currentSteps.add(step);
    }

    @Override
    public void endOfScenarioLifeCycle(final Scenario scenario) {
    }

    @Override
    public void done() {
    }

    @Override
    public void close() {
    }

    @Override
    public void eof() {
    }

    @Override
    public void before(final Match match, final Result result) {
        if (result.getError() != null) {
            write("<span class='error'>Error before scenario: " + result.getError().getMessage() + "; see Execution Log for details</span>");
            errorPrinter.write(result.getErrorMessage());
        }
    }

    @Override
    public void result(final Result result) {
        Step currentStep = currentSteps.poll();
        String status = result.getStatus();
        if (Result.PASSED.equals(status)) {
            processStep(currentStep, ExecutionResult.PASS);
        } else if (Result.FAILED.equals(status)) {
            processStep(currentStep, ExecutionResult.FAIL);
        } else if (Result.SKIPPED.getStatus().equals(status)) {
            processStep(currentStep, ExecutionResult.IGNORE);
        } else if (Result.UNDEFINED.getStatus().equals(status)) {
            processUndefinedStep(currentStep);
        } else {
            processStep(currentStep, ExecutionResult.ERROR);
        }
    }

    @Override
    public void after(final Match match, final Result result) {
        if (result.getError() != null) {
            write("<span class='error'>Error after scenario: " + result.getError().getMessage() + "; see Execution Log for details</span>");
            errorPrinter.write(result.getErrorMessage());
        }
    }

    @Override
    public void match(final Match match) {
    }

    @Override
    public void embedding(final String mimeType, final byte[] data) {
    }

    @Override
    public void write(final String text) {
        outputPrinter.write(text);
    }

    private void processStep(Step step, ExecutionResult result) {
        if (testSummary != null) testSummary.add(result);
        write(format("<span class='%s'>%s%s</span><br/>", result.name().toLowerCase(), step.getKeyword(), escapeHTML(step.getName())));
    }

    private void processUndefinedStep(final Step step) {
        if (testSummary != null) testSummary.add(ExecutionResult.ERROR);
        write(format("<span class='error'>Undefined step: %s%s</span><br/>", step.getKeyword(), escapeHTML(step.getName())));
    }

}
