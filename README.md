<div align="center">

# 🏆 Arte & Metal — ERP

### Sistema de Gestión Empresarial para Orfebrería y Joyería

**Arte y Metal Chiclayo E.I.R.L.**

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-26-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

</div>

---

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Stack Tecnológico](#-stack-tecnológico)
- [Arquitectura del Sistema](#-arquitectura-del-sistema)
- [Módulos del Sistema](#-módulos-del-sistema)
- [Flujo de Trabajo](#-flujo-de-trabajo)
- [Modelo de Datos](#-modelo-de-datos)
- [Seguridad y Roles](#-seguridad-y-roles)
- [Capturas de Pantalla](#-capturas-de-pantalla)
- [Instalación y Configuración](#-instalación-y-configuración)
- [Credenciales por Defecto](#-credenciales-por-defecto)
- [API REST](#-api-rest)
- [Equipo](#-equipo)
- [Curso](#-curso)
- [Licencia](#-licencia)

---

## 📖 Descripción

**Arte & Metal ERP** es un sistema web integral desarrollado para la gestión completa de una empresa de orfebrería y joyería. Cubre todo el ciclo de vida de un pedido personalizado — desde el registro del cliente y la cotización, pasando por el diseño, la producción, el almacenamiento, la distribución y la venta final en punto de venta (POS).

El sistema está diseñado para optimizar la comunicación entre los distintos roles del taller: **vendedores**, **diseñadores**, **orfebres**, **almaceneros** y **repartidores**, asegurando trazabilidad total de cada pieza.

> 🎯 **Objetivo:** Digitalizar y centralizar los procesos operativos de una joyería artesanal, reemplazando hojas de cálculo y papeles por una plataforma web robusta, segura y en tiempo real.

---

## 🛠 Stack Tecnológico

| Categoría         | Tecnología                                                                                                             |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **Lenguaje**      | ![Java](https://img.shields.io/badge/Java_26-ED8B00?logo=java&logoColor=white)                                         |
| **Framework**     | ![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5.6-6DB33F?logo=springboot&logoColor=white)                  |
| **ORM**           | ![Hibernate](https://img.shields.io/badge/Hibernate-59666C?logo=hibernate&logoColor=white) / JPA                       |
| **Base de Datos** | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL_17-4169E1?logo=postgresql&logoColor=white)                       |
| **Frontend**      | ![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?logo=thymeleaf&logoColor=white) + HTML5 + CSS3 + JavaScript |
| **Seguridad**     | ![Spring Security](https://img.shields.io/badge/Spring_Security_6-6DB33F?logo=springsecurity&logoColor=white)          |
| **Build**         | ![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white)                                   |
| **API Externa**   | [Decolecta API](https://api.decolecta.com) — RENIEC (DNI) + SUNAT (RUC)                                                |

---

## 🏗 Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    Cliente (Navegador)                      │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP (Thymeleaf Templates)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Web Server                    │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐ │
│  │ Controllers  │  │   Services   │  │  Security          │ │
│  │ (MVC + REST) │──▶  (Business   │──▶  (Spring Security) │ │
│  └──────────────┘  │   Logic)     │  └────────────────────┘ │
│                    └──────┬───────┘                         │
│                           │                                 │
│                    ┌──────▼────────┐                        │
│                    │  Repositories │                        │
│                    │  (Spring JPA) │                        │
│                    └──────┬────────┘                        │
└───────────────────────────┼─────────────────────────────────┘
                            │
                     ┌──────▼────────┐
                     │  PostgreSQL   │
                     │   (arteymetal)│
                     └───────────────┘
```

### Patrón de Diseño: **MVC (Model-View-Controller)**

- **Model:** Entidades JPA (`entity/`) + Repositorios (`repository/`)
- **View:** Plantillas Thymeleaf (`templates/`)
- **Controller:** Controladores MVC y REST (`controller/`)
- **Service:** Lógica de negocio (`service/`)
- **Config:** Seguridad, datos iniciales, advice global (`config/`)

---

## 📦 Módulos del Sistema

<details open>
<summary><strong>🔐 Autenticación y Gestión de Usuarios</strong></summary>

- Login por username o email con **rate limiting** (6 intentos/60s)
- Registro de usuarios, recuperación de contraseña
- Perfil de usuario: actualizar datos, cambiar contraseña, eliminar cuenta
- CRUD de usuarios (administradores)
- Roles y permisos granulares (25 permisos, 6 roles)
</details>

<details open>
<summary><strong>👥 Clientes</strong></summary>

- Registro completo de clientes (nombre, documento, dirección, contacto)
- Búsqueda por documento
- Consulta automática de **DNI (RENIEC)** y **RUC (SUNAT)** vía Decolecta API
- Asociación con pedidos e historial de compras
</details>

<details open>
<summary><strong>💍 Pedidos Personalizados</strong></summary>

- Creación de pedidos con productos personalizados (descripción, material, medidas)
- Asignación de cliente, vendedor, tipo de entrega (local/delivery)
- Gestión de estados: `registrado → en_producción → produciendo → listo_entrega → en_transporte → en_almacén → listo_recoger → entregado`
- Personalización del diseño: `sin_iniciar → en_diseño → en_revisión → completado`
- Control de pagos: `pendiente_adelanto → adelanto_pagado → pagado_completo`
- Subida de archivos: diseños, órdenes de compra
- Adjuntar hasta 50MB por archivo
</details>

<details open>
<summary><strong>🎨 Diseño</strong></summary>

- Visualización de pedidos pendientes de diseño
- Subida y gestión de archivos de diseño (PDF, imágenes)
- Cambio de estado de personalización
- Notificaciones automáticas a vendedores al completar diseño
</details>

<details open>
<summary><strong>🔧 Producción</strong></summary>

- Pedidos listos para producción
- Inicio de producción con registro de fecha
- Notificación a repartidor cuando el pedido está listo
- Vista detallada con archivos de diseño y especificaciones
</details>

<details open>
<summary><strong>📦 Almacén</strong></summary>

- Inventario con stock dual: **tienda** + **almacén**
- Movimientos de entrada y salida con trazabilidad
- Recepción de pedidos provenientes de producción
- Despacho de pedidos para entrega
- Control de stock mínimo
</details>

<details open>
<summary><strong>🚚 Repartidor / Delivery</strong></summary>

- Recojo de pedidos desde producción
- Entrega a almacén
- Seguimiento de pedidos en transporte
- Gestión de direcciones de entrega
</details>

<details open>
<summary><strong>💳 Ventas (POS)</strong></summary>

- Punto de venta desde stock en tienda
- Selección de caja activa (sesión por caja)
- Cálculo automático de vuelto
- Múltiples métodos de pago: efectivo + digital
- Emisión de comprobantes: boleta o factura
- Historial de ventas por caja apertura
</details>

<details open>
<summary><strong>💰 Caja Chica</strong></summary>

- Apertura y cierre de caja
- Seguimiento de monto inicial / final
- Totales de ventas por sesión
- Sesión persistente en HTTP Session
</details>

<details open>
<summary><strong>📊 Dashboard y Reportes</strong></summary>

- Dashboard con KPIs en tiempo real:
  - Total clientes, productos, pedidos activos, ventas del día
  - Gráfico de ventas de los últimos 14 días
  - Gráfico de pedidos por estado
- Reportes exportables:
  - Ventas por rango de fechas (CSV/XLSX)
  - Pedidos con saldos pendientes
  - Productos con stock bajo
  - Reporte detallado de ventas
  </details>

<details open>
<summary><strong>🔔 Notificaciones</strong></summary>

- Sistema de notificaciones en-app
- Notificaciones por cambios de estado en pedidos
- Conteo no leído vía AJAX
- Acciones rápidas desde la notificación
</details>

---

## 🔄 Flujo de Trabajo

```
                ┌──────────────┐
                │   CLIENTE    │
                │  (Registro)  │
                └──────┬───────┘
                       │
                ┌──────▼───────┐
                │   VENDEDOR   │
                │ (Crea Pedido)│
                └──────┬───────┘
                       │
          ┌────────────┼────────────┐
          │            │            │
   ┌──────▼──────┐ ┌───▼─────┐ ┌───▼──────┐
   │  DISEÑADOR  │ │ ORFEBRE │ │ALMACENERO│
   │(Sube Diseño)│ │(Produce)│ │(Recibe)  │
   └──────┬──────┘ └───┬─────┘ └───┬──────┘
          │            │          │
          └────────────┼──────────┘
                       │
                ┌──────▼───────┐
                │  REPARTIDOR  │
                │   (Delivery) │
                └──────┬───────┘
                       │
                ┌──────▼───────┐
                │   VENDEDOR   │
                │  (Entrega)   │
                └──────────────┘
```

---

## 🗄 Modelo de Datos

### Principales Entidades

```
┌───────────┐     ┌──────────────┐     ┌───────────┐
│  Usuario  │────▶│     Rol      │◀────│  Permiso  │
└───────────┘     └──────────────┘     └───────────┘
      │
      │ 1
      ▼
┌───────────┐     ┌──────────────┐     ┌───────────┐
│  Pedido   │◀───▶│  Cliente     │     │ Producto  │
└───────────┘     └──────────────┘     └───────────┘
      │                                       │
      │ 1                                     │ 1
      ▼                                       ▼
┌────────────┐    ┌──────────────┐     ┌───────────┐
│VentaDetalle│    │ Venta        │     │ProdImagen │
└────────────┘    └──────┬───────┘     └───────────┘
                         │
                    ┌────▼───────┐
                    │CajaApertura│
                    └────┬───────┘
                         │
                    ┌────▼───────┐
                    │    Caja    │
                    └────────────┘
```

### 20 Repositorios JPA | 19 Entidades | 22 Controladores | 9 Servicios

---

## 🛡 Seguridad y Roles

### Roles del Sistema

| Rol                  | Acceso Principal                                          |
| -------------------- | --------------------------------------------------------- |
| 👑 **Administrador** | Acceso total al sistema, gestión de usuarios y roles      |
| 💼 **Vendedor**      | CRUD clientes, crear/editar pedidos, ventas POS, reportes |
| 🎨 **Diseñador**     | Gestión de diseños, subida de archivos, cambio de estados |
| 🔧 **Orfebre**       | Producción, inicio y finalización de pedidos en taller    |
| 📦 **Almacenero**    | Inventario, movimientos, recepción y despacho             |
| 🚚 **Repartidor**    | Recojo, entrega a almacén, gestión de delivery            |

### Esquema de Permisos (25 permisos)

```
dashboard.ver        pedidos.ver         clientes.ver
pedidos.gestionar    pedidos.eliminar    clientes.gestionar
productos.ver        productos.gestionar productos.eliminar
ventas.ver           ventas.realizar     ventas.eliminar
caja.ver             almacen.ver         almacen.gestionar
produccion.ver       produccion.gestionar diseno.ver
diseno.gestionar     repartidor.ver      repartidor.gestionar
reportes.ver         reportes.exportar   roles.gestionar
usuarios.gestionar
```

### Medidas de Seguridad

- ✅ Autenticación con **BCrypt**
- ✅ Rate limiting en login (6 intentos en 60 segundos)
- ✅ Autorización granular por permiso
- ✅ Sesiones HTTP seguras
- ✅ Validación de datos en servidor (Jakarta Validation)
- ✅ Protección contra CSRF (Spring Security)
- ✅ Página personalizada de acceso denegado

---

## 📸 Capturas de Pantalla

> _Próximamente..._

| Módulo     | Vista |
| ---------- | ----- |
| Dashboard  | ⬜    |
| Pedidos    | ⬜    |
| POS Ventas | ⬜    |
| Producción | ⬜    |
| Reportes   | ⬜    |

---

## 🚀 Instalación y Configuración

### Prerrequisitos

- [JDK 26](https://jdk.java.net/26/) o superior
- [Apache Maven](https://maven.apache.org/) 3.9+
- [PostgreSQL](https://www.postgresql.org/) 17+
- Git

### Pasos

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-usuario/arteymetal-erp.git
cd arteymetal-erp

# 2. Crear la base de datos
psql -U postgres -c "CREATE DATABASE arteymetal;"

# 3. Configurar variables de entorno (opcional)
export DB_URL=jdbc:postgresql://localhost:5432/arteymetal
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export DECOLECTA_API_KEY=tu-api-key

# 4. Compilar y ejecutar
mvn clean install
mvn spring-boot:run

# 5. Abrir en el navegador
open http://localhost:8081
```

> **Nota:** Si no configuras variables de entorno, el sistema usará los valores por defecto de `application.properties`.

### Configuración Adicional

| Propiedad               | Descripción                           | Valor por Defecto                             |
| ----------------------- | ------------------------------------- | --------------------------------------------- |
| `server.port`           | Puerto del servidor                   | `8081`                                        |
| `spring.datasource.url` | URL de conexión a PostgreSQL          | `jdbc:postgresql://localhost:5432/arteymetal` |
| `app.upload.dir`        | Directorio de subida de archivos      | `uploads/`                                    |
| `decolecta.api-key`     | API Key para Decolecta (RENIEC/SUNAT) | _(requerido)_                                 |

---

## 🔑 Credenciales por Defecto

> ⚠️ **Importante:** Cambia estas credenciales en producción.

| Rol                  | Email                       | Contraseña      |
| -------------------- | --------------------------- | --------------- |
| 👑 **Administrador** | `bvasquezkeysije@gmail.com` | `76636255`      |
| 👑 **Administrador** | `pfernandezadeli@gmail.com` | `77684878`      |
| 💼 **Vendedor**      | `ventas@gmail.com`          | `ventas123`     |
| 🔧 **Orfebre**       | `produccion@gmail.com`      | `produccion123` |
| 📦 **Almacenero**    | `almacen@gmail.com`         | `almacen123`    |
| 🎨 **Diseñador**     | `disenador@gmail.com`       | `disenador123`  |
| 🚚 **Repartidor**    | `repartidor@gmail.com`      | `repartidor123` |

---

## 🌐 API REST

El sistema expone endpoints REST para consultas externas:

| Endpoint                       | Método | Descripción                               |
| ------------------------------ | ------ | ----------------------------------------- |
| `/cliente-consulta/dni/{dni}`  | GET    | Consulta DNI (RENIEC) vía Decolecta API   |
| `/cliente-consulta/ruc/{ruc}`  | GET    | Consulta RUC (SUNAT) vía Decolecta API    |
| `/sunat/consulta/{ruc}`        | GET    | Consulta RUC (endpoint legacy)            |
| `/notificaciones/unread-count` | GET    | Conteo de notificaciones no leídas (AJAX) |

---

## 👥 Equipo

| Integrante                                                                             | Rol                       |
| -------------------------------------------------------------------------------------- | ------------------------- |
| <img src="https://github.com/identicons/ade.png" width="24" height="24"> **Adelit**    | Desarrolladora Full Stack |
| <img src="https://github.com/identicons/mafer.png" width="24" height="24"> **Mafer**   | Desarrolladora Backend    |
| <img src="https://github.com/identicons/manuel.png" width="24" height="24"> **Manuel** | Desarrollador Frontend    |
| <img src="https://github.com/identicons/jhan.png" width="24" height="24"> **Jhan**     | Desarrollador Frontend    |

---

## 🎓 Curso

> **Desarrollo de Aplicaciones** — Universidad

Proyecto integrador del curso Desarrollo de Aplicaciones, aplicando metodologías ágiles y buenas prácticas de ingeniería de software para construir un sistema ERP completo y funcional.

---

## 📄 Licencia

Este proyecto está bajo la licencia **MIT**. Ver el archivo [LICENSE](LICENSE) para más detalles.

---

<div align="center">

### Hecho con ❤️ por el Grupo 3

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/)

</div>
