package scaffolding;

import org.eclipse.jetty.client.api.ContentResponse;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import static org.hamcrest.CoreMatchers.is;

public class ContentResponseMatcher {
    public static Matcher<ContentResponse> equalTo(int statusCode, Matcher<String> contentMatcher) {
        Matcher<Integer> statusCodeMatcher = is(statusCode);
        return new TypeSafeDiagnosingMatcher<ContentResponse>() {
            protected boolean matchesSafely(ContentResponse t, Description description) {
                boolean matches = true;
                if (!statusCodeMatcher.matches(t.getStatus())) {
                    description.appendText("statusCode ");
                    statusCodeMatcher.describeMismatch(t.getStatus(), description);
                    matches = false;
                }

                if (!contentMatcher.matches(t.getContentAsString())) {
                    if (!matches)
                        description.appendText(", ");

                    description.appendText("content ");
                    contentMatcher.describeMismatch(t.getContentAsString(), description);
                    matches = false;
                }

                return matches;
            }

            public void describeTo(Description description) {
                description
                    .appendText("{statusCode ")
                    .appendDescriptionOf(statusCodeMatcher)
                    .appendText(", content ")
                    .appendDescriptionOf(contentMatcher)
                    .appendText("}");
            }
        };
    }
}
