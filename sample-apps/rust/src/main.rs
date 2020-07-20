use actix_web::{web, middleware, App, HttpResponse, HttpServer, Responder};
use std::env;

async fn index() -> impl Responder {
    HttpResponse::Ok().body("<html><h1>Rust sample app is running</h1></html>")
}

#[actix_rt::main]
async fn main() -> std::io::Result<()> {

    let app_name = env::var("APP_NAME").unwrap_or(String::from("rust-sample"));
    let app_port = env::var("APP_PORT").unwrap_or(String::from("8088"));
    let app_data = env::var("APP_DATA").unwrap_or(String::from(""));

    println!("Started rust app '{}' on port '{}' with data dir of '{}'", app_name, app_port, app_data);

    HttpServer::new(move || {
        let app_root = format!("/{}/", app_name);
        App::new()
            .wrap(middleware::NormalizePath) //Tolerates lack/presence of trailing slash in URL
            .route("/", web::get().to(index))
            .route(&app_root[..], web::get().to(index))
    })
    .bind(format!("127.0.0.1:{}",app_port))?
    .run()
    .await
}
