## Configuracion

### H2 Console

H2 Console es una web-bbdd interfaz para interactuar con la bbdd de H2

#### H2 Console - consola

1. en el browser ir a http://localhost:8080/h2-console
2. **Login** using the following credentials:

    - **Saved Settings**: `Generic H2 (Embedded)`
    - **Driver Class**: `org.h2.Driver`
    - **JDBC URL**: `jdbc:h2:mem:urlshortener`
    - **User Name**: `sa`
    - **Password**: (leave blank)

#### Example Configuration

Here is a summary of the configuration settings:

| Setting Name          | Value                           |
|-----------------------|---------------------------------|
| Driver Class          | `org.h2.Driver`                 |
| JDBC URL              | `jdbc:h2:mem:urlshortener`      |
| User Name             | `sa`                            |
| Password              | (leave blank)                   |

## Uso

### Crear un ShortURL

Para crear un shortURL, enviar un POST request a `/api/urls` endpoint.
   
   
