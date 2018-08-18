using Microsoft.AspNetCore;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using System.Collections;
using System.Collections.Generic;

namespace dotnet_sample
{
    public class Program
    {
        public static void Main(string[] args)
        {
            var envs = System.Environment.GetEnvironmentVariables();

            var webhostBuilder = CreateWebHostBuilder(args);

            SetupAppRunnerEnvironment(webhostBuilder, envs);

            webhostBuilder.Build().Run();
        }

        public static void SetupAppRunnerEnvironment(IWebHostBuilder webhostBuilder, IDictionary envs)
        {
            if (envs.Contains("APP_PORT"))
            {
                var port = int.Parse(envs["APP_PORT"].ToString());
                webhostBuilder.UseKestrel(options =>
                {
                    options.ListenAnyIP(port);
                });
            }

            if (envs.Contains("APP_NAME"))
            {
                //app runner expect the service to be host at http(s)://host:port/APP_NAME
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.AddInMemoryCollection(new List<KeyValuePair<string, string>>() {
                    new KeyValuePair<string, string>("PathBase", envs["APP_NAME"].ToString())
                });
                var configration = builder.Build();
                webhostBuilder.UseConfiguration(configration);
            }
        }

        public static IWebHostBuilder CreateWebHostBuilder(string[] args)
        {
            return WebHost.CreateDefaultBuilder(args)
                          .UseStartup<Startup>();
        }
    }
}
