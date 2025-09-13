# Comunicación-Cliente-Servidor
Entregable de la materia de Tópicos para el despliegue de aplicaciones, donde se usa un protocolo para la comunicación entre clientes a través de un servidor.

Integrantes:

•	Gómez Juárez Alan Fabricio

•	Luna García Erika Josabet

•	Navarro Negrete María Fernanda

Descripción
Sistema de chat en tiempo real que permite comunicación grupal (broadcast) y privada entre múltiples usuarios conectados a un servidor central. Implementado en Python con interfaz gráfica tkinter y comunicación mediante sockets TCP con protocolo JSON.
Arquitectura

•	Servidor Central: Gestiona todas las conexiones y mensajes

•	Clientes Múltiples: Interfaz gráfica para cada usuario

•	Protocolo JSON: Comunicación estructurada y extensible

•	Conexiones TCP: Comunicación confiable y en tiempo real

Protocolo de Comunicación JSON

Estructura Básica

{"cmd": "comando", "param1": "valor1", "param2": "valor2"}

•	Formato: JSON
•	Encoding: UTF-8
•	Transporte: TCP
•	Separador: No necesario (JSON nativo)
Comandos Cliente → Servidor
1. LOGIN - Autenticación de usuario
{"cmd": "login", "user": "nombre_usuario"}
Propósito: Registrar usuario en el servidor
Parámetros:
•	user: Nombre único del usuario
Ejemplo:
{"cmd": "login", "user": "Ana123"}
2. BROADCAST - Mensaje al chat general
{"cmd": "broadcast", "msg": "mensaje_texto"}
Propósito: Enviar mensaje a todos los usuarios
Parámetros:
•	msg: Contenido del mensaje
Ejemplo:
{"cmd": "broadcast", "msg": "¡Hola a todos!"}
3. MESSAGE - Mensaje privado
{"cmd": "message", "to": "destinatario", "msg": "mensaje_texto"}
Propósito: Enviar mensaje privado a usuario específico
Parámetros:
•	to: Usuario destinatario
•	msg: Contenido del mensaje
Ejemplo:
{"cmd": "message", "to": "Carlos", "msg": "¿Vamos al cine?"}
4. USERS - Solicitar lista de usuarios
{"cmd": "users"}
Propósito: Obtener lista de usuarios conectados
5. TYPING - Indicador de escritura
{"cmd": "typing"}
Propósito: Notificar que el usuario está escribiendo
Comandos Servidor → Cliente
1. HISTORY - Historial del chat
{"cmd": "history", "items": [{"time": "HH:MM:SS", "from": "usuario", "msg": "mensaje"}, ...]}
Propósito: Enviar historial al usuario que se conecta
Ejemplo:
{
  "cmd": "history",
  "items": [
    {"time": "10:30:15", "from": "Ana", "msg": "Buenos días"},
    {"time": "10:31:22", "from": "Carlos", "msg": "¿Cómo están?"}
  ]
}
2. BROADCAST - Mensaje grupal entrante
{"cmd": "broadcast", "from": "remitente", "msg": "mensaje_texto"}
Propósito: Distribuir mensaje grupal a todos los usuarios
Ejemplo:
{"cmd": "broadcast", "from": "Ana", "msg": "¡Hay reunión a las 3PM!"}
3. PRIVATE - Mensaje privado entrante
{"cmd": "private", "from": "remitente", "msg": "mensaje_texto"}
Propósito: Entregar mensaje privado a destinatario
Ejemplo:
{"cmd": "private", "from": "Carlos", "msg": "Te espero en la cafetería"}
4. USERS - Lista de usuarios actualizada
{"cmd": "users", "list": ["usuario1", "usuario2", "usuario3"]}
Propósito: Actualizar lista de usuarios conectados
Ejemplo:
{"cmd": "users", "list": ["Ana", "Carlos", "Luis"]}
5. SYSTEM - Mensaje del sistema
{"cmd": "system", "msg": "mensaje_sistema"}
Propósito: Notificaciones del servidor
Ejemplo:
{"cmd": "system", "msg": "Carlos se ha unido al chat"}
6. TYPING - Indicador de escritura
{"cmd": "typing", "user": "nombre_usuario"}
Propósito: Notificar que un usuario está escribiendo
Ejemplo:
{"cmd": "typing", "user": "Ana"}
Ejecución
Ejecutar Servidor
python Servidor.py
Ejecutar Cliente
python Cliente.py
Funcionalidades Implementadas
•	✅ Chat grupal (broadcast) en tiempo real
•	✅ Chat privado entre usuarios
•	✅ Lista dinámica de usuarios conectados
•	✅ Historial de mensajes al conectar
•	✅ Indicador "escribiendo..."
•	✅ Notificaciones de conexión/desconexión
•	✅ Interfaz gráfica con pestañas
•	✅ Contador de mensajes no leídos
Flujo de Comunicación
1.	Conexión: Cliente envía login → Servidor responde con history y users
2.	Mensajería:
o	Broadcast: broadcast → broadcast (a todos)
o	Privado: message → private (solo al destinatario)
3.	Actualizaciones: Servidor envía users automáticamente cuando cambian las conexiones
