package com.danielflower.apprunner;

import com.danielflower.apprunner.mgmt.ValidationException;

public class EntryPoint {

    public static void main(String[] args) {
        start(args, null);
    }

    public static void start(String[] args, AppRunnerHooks hooks) {
        try {
            Config config = Config.load(args);
            config.hooks = hooks != null ? hooks : createHooks(config);
            App app = new App(config);
            app.start();
            Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
        } catch (Throwable t) {
            App.log.error("Error during startup", t);
            System.exit(1);
        }
    }

    private static AppRunnerHooks createHooks(Config config) {
        String gitUrlValidationRegex = config.get(Config.GIT_URL_VALIDATION_REGEX, "");
        String gitUrlValidationMessage = config.get(Config.GIT_URL_VALIDATION_MESSAGE, "Invalid git URL");
        return new AppRunnerHooks() {
            @Override
            public void validateGitUrl(String gitUrl) throws ValidationException {
                if (!gitUrlValidationRegex.isEmpty()) {
                    if (!gitUrl.matches(gitUrlValidationRegex)) {
                        throw new ValidationException(gitUrlValidationMessage);
                    }
                }
            }
        };
    }
}
