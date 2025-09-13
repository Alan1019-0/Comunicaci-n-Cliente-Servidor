# Comunicación-Cliente-Servidor
Entregable de la materia de Tópicos para el despliegue de aplicaciones, donde se usa un protocolo para la comunicación entre clientes a través de un servidor.
## Integrantes
- Gómez Juárez Alan Fabricio  
- Luna García Erika Josabet  
- Navarro Negrete María Fernanda  
## Descripción
Sistema de chat en tiempo real que permite comunicación grupal (broadcast) y privada entre múltiples usuarios conectados a un servidor central. Implementado en Python y Java con interfaces gráficas y comunicación mediante sockets TCP con protocolo JSON
## Arquitectura
- **Servidor Central**: Gestiona todas las conexiones y mensajes (Python)  
- **Clientes Múltiples**: Interfaz gráfica para cada usuario (Python y Java)  
- **Protocolo JSON**: Comunicación estructurada y extensible  
- **Conexiones TCP**: Comunicación confiable y en tiempo real  

## Protocolo de Comunicación JSON

### Estructura Básica
{"cmd": "comando", "param1": "valor1", "param2": "valor2"}
Formato: JSON

Encoding: UTF-8

Transporte: TCP

Separador: No necesario (JSON nativo)
## Comandos Cliente → Servidor
### LOGIN - Autenticación de usuario
{"cmd": "login", "user": "nombre_usuario"}

Propósito: Registrar usuario en el servidor

### BROADCAST - Mensaje al chat general
{"cmd": "broadcast", "msg": "mensaje_texto"}

Propósito: Enviar mensaje a todos los usuarios

### MESSAGE - Mensaje privado
{"cmd": "message", "to": "destinatario", "msg": "mensaje_texto"}

Propósito: Enviar mensaje privado a usuario específico

### USERS - Solicitar lista de usuarios
{"cmd": "users"}

### TYPING - Indicador de escritura
{"cmd": "typing"}

## Comandos Servidor → Cliente
### HISTORY - Historial del chat
{
  "cmd": "history",
  "items": [
    {"time": "10:30:15", "from": "Ana", "msg": "Buenos días"},
    {"time": "10:31:22", "from": "Carlos", "msg": "¿Cómo están?"}
  ]
}

### BROADCAST - Mensaje grupal entrante
{"cmd": "broadcast", "from": "Ana", "msg": "¡Hay reunión a las 3PM!"}

### PRIVATE - Mensaje privado entrante
{"cmd": "private", "from": "Carlos", "msg": "Te espero en la cafetería"}

### USERS - Lista de usuarios actualizada
{"cmd": "users", "list": ["Ana", "Carlos", "Luis"]}

### SYSTEM - Mensaje del sistema
{"cmd": "system", "msg": "Carlos se ha unido al chat"}

TYPING - Indicador de escritura
{"cmd": "typing", "user": "Ana"}
## Ejecución
Servidor (Python) (Primero)

python Servidor.py

Cliente Python

python Cliente.py

Cliente Java
# Compilar
javac Cliente.java

# Ejecutar
java Cliente

## Características del Cliente Java
- Interfaz gráfica con SplitPane
- Panel de usuarios conectados
- Área de chat con historial
- Soporte para mensajes privados
- Indicador de "escribiendo..."
- Timestamps en mensajes
- Auto-scroll al final del chat
- Pantalla de Conexión

Al ejecutar el cliente Java, se muestra una ventana para ingresar:

- Nombre de usuario
- Dirección del host (por defecto: localhost)
- Puerto (por defecto: 5000)
## Funcionalidades Implementadas
### Comunes a ambos clientes
- Chat grupal (broadcast) en tiempo real
- Chat privado entre usuarios
- Lista dinámica de usuarios conectados
- Historial de mensajes al conectar
- Indicador "escribiendo..."
- Notificaciones de conexión/desconexión
### Específicas del Cliente Python
- Interfaz con pestañas para chats privados
- Contador de mensajes no leídos
- Panel lateral de usuarios
### Específicas del Cliente Java
- Interfaz con SplitPane
- Botón específico para mensajes privados
- Barra de estado con información de conexión
- Timestamps en todos los mensajes
## Flujo de Comunicación
- Conexión: Cliente envía login → Servidor responde con history y users.
- Mensajería:
- Broadcast: broadcast → broadcast (a todos)
- Privado: message → private (solo al destinatario)
- Actualizaciones: Servidor envía users automáticamente cuando cambian las conexiones.

## Interoperabilidad
- Los clientes Python y Java pueden:
- Conectarse al mismo servidor simultáneamente
- Comunicarse entre sí sin problemas
- Recibir la misma lista de usuarios
-Participar en chats grupales y privados de forma cruzada
