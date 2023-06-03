using Microsoft.Extensions.Configuration;
var envs = System.Environment.GetEnvironmentVariables();
var builder = WebApplication.CreateBuilder(args);

//set app port
if(envs.Contains("APP_PORT")){
    builder.WebHost.ConfigureKestrel(options=>options.ListenLocalhost(int.Parse(envs["APP_PORT"].ToString())));
}

// Add services to the container.

builder.Services.AddControllers();
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

//set app name

app.UsePathBase($"/{envs["APP_NAME"].ToString()}");

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

app.UseRouting();

app.MapGet("/",()=>"Hello World!");
app.Run();
