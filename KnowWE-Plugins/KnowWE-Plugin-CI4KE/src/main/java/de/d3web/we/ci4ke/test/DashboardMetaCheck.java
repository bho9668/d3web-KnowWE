package de.d3web.we.ci4ke.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.denkbares.strings.NumberAwareComparator;
import de.d3web.testing.AbstractTest;
import de.d3web.testing.Message;
import de.d3web.testing.TestResult;
import de.d3web.we.ci4ke.dashboard.CIDashboard;
import de.d3web.we.ci4ke.dashboard.CIDashboardManager;
import de.d3web.we.ci4ke.dashboard.type.CIDashboardType;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.utils.KnowWEUtils;

/**
 * This test Checks all other Dashboards for failed tests and lists them
 *
 * @author Nikolai Reed (Olyro GmbH)
 * @created 24.10.2018
 */

public class DashboardMetaCheck extends AbstractTest<Article> {

    @Override
    public Message execute(Article article, String[] args, String[]... ignores) throws InterruptedException {
        List<Section<CIDashboardType>> successors = Sections.successors(article, CIDashboardType.class);
        Set<String> failedTests = new HashSet<>();
        for (Section<CIDashboardType> successor : successors) {
            CIDashboard dashboard = CIDashboardManager.getDashboard(successor);
            if (dashboard == null )
                continue;
            Message.Type overallResult = dashboard.getLatestBuild().getOverallResult();
            String test = overallResult.name();
            if (overallResult != Message.Type.SUCCESS)
            {
                for (TestResult result : dashboard.getLatestBuild().getResults()) {
                    if (result.getTestName() != this.getName()) {
                        if (!result.isSuccessful())
                            failedTests.add(result.getTestName());
                    }
                }
                if (failedTests.size() > 0)
                    return createDashboardError(dashboard.getDashboardName(), failedTests);
            }
        }
        return new Message(Message.Type.SUCCESS, null);
    }

    protected Message createDashboardError(String dashboard, Set<String> testNames) {
        // create error message for the build report
        StringBuilder result = new StringBuilder();
        result.append("Dashbord ")
                .append(KnowWEUtils.maskJSPWikiMarkup(dashboard));
        result.append("\nThe following tests have failed:");
        testNames.stream().sorted(NumberAwareComparator.CASE_INSENSITIVE)
                .forEach(name -> result.append("\n* ").append(KnowWEUtils.maskJSPWikiMarkup(name)));
        return new Message(Message.Type.FAILURE, result.toString());
    }


    @Override
    public Class<Article> getTestObjectClass() {
        return Article.class;
    }

    @Override
    public String getDescription() {
        return "Checks all other Dashboards for failed tests.";
    }
}