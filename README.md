[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/IEOUmR9z)

# 🎓 Trabajo Final 2025 - Sistema de Asistencia a Eventos

Este repositorio contiene el desarrollo del **Trabajo Final** para la materia **Programación II**.  
El objetivo es construir un **sistema distribuido** para el registro y gestión de asistencia a eventos únicos, como charlas, cursos u obras de teatro.

---

## 🎯 Objetivo General
El sistema permitirá a los usuarios:

✅ Ver un listado de eventos  
✅ Seleccionar asientos  
✅ Confirmar la compra de entradas  

El desarrollo se divide en **tres componentes principales**, todos implementados por el alumno.

---

## 🏛️ Arquitectura del Sistema
El sistema está compuesto por servicios **provistos por la cátedra** y servicios **desarrollados por el alumno**.

### 🧱 Componentes de la Cátedra (Provistos)

| Servicio | Descripción |
|----------|-------------|
| **API REST (Java)** | Gestiona la lógica de negocio principal (eventos, ventas, bloqueos). |
| **Kafka** | Notifica cambios en tiempo real sobre los eventos. |
| **Redis** | Almacena el estado de los asientos (libres, ocupados, bloqueados). |

---

### 🛠️ Componentes del Alumno (A desarrollar)

#### 1️⃣ Backend del Alumno (`backend`)
- Servicio principal desarrollado en **Java con Spring Boot** (idealmente JHipster).
- Se comunica con el servicio de la cátedra y con el proxy.
- Expone una **API REST** para el cliente móvil.
- Usa **MySQL local** para persistir ventas y usuarios.
- Usa **Redis local** para gestión de sesiones de usuario.

#### 2️⃣ Servicio Proxy (`proxy`)
- Servicio intermediario desarrollado en **Java**.
- Es el **único componente con acceso al Kafka y Redis de la cátedra**.
- Se suscribe al tópico Kafka, recibe notificaciones y las reenvía al backend.
- Expone una API para que el backend consulte el estado de los asientos.

#### 3️⃣ Cliente Móvil (`frontend`)
- Aplicación desarrollada en **Kotlin Multiplatform (KMP)**.
- Es la interfaz gráfica del sistema.
- Consume la API REST del backend.
- Permite: iniciar sesión, ver eventos, seleccionar asientos y confirmar compras.

> 📌 *(Aquí va la imagen del diagrama de arquitectura cuando esté disponible)*

---

## 🚀 Tecnologías Principales

| Componente | Tecnologías |
|------------|-------------|
| **Backend** | Java, Spring Boot, Spring Data JPA, Spring Security (JWT) |
| **Proxy** | Java, Spring Boot, Spring Kafka, Spring Data Redis |
| **Cliente Móvil** | Kotlin Multiplatform (KMP) |
| **DB Local** | MySQL |
| **Cache / Estado** | Redis |
| **Mensajería** | Apache Kafka |
| **Comunicación** | API REST (JSON) + Autenticación vía JWT |

---

## 🛠️ Cómo ejecutar (Ejemplo)

> 📌 *Aún en desarrollo. Las instrucciones finales se documentarán más adelante.*

### ✅ 1. Prerrequisitos
- Java (JDK 17+)
- Docker y Docker Compose (para bases de datos y Kafka)
- Android Studio (para el cliente móvil)

### ▶️ 2. Backend

```bash
cd backend/
./mvnw spring-boot:run
```

### ▶️ 3. Proxy

```bash
cd proxy/
./mvnw spring-boot:run
```

### ▶️ 4. Cliente Móvil

Abrir la carpeta frontend/ con Android Studio y ejecutar en un emulador o dispositivo físico.