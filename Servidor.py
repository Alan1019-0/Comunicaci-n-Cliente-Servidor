# Servidor.py
import socket
import threading
import json
import time
from datetime import datetime

class Servidor:
    """
    Servidor TCP simple que usa mensajes JSON para un chat:
    - comandos entrantes: login, broadcast, message, users, typing
    - notifica cambios de usuarios y envía historial del chat general al loguearse
    """

    def __init__(self, host="0.0.0.0", port=5000, max_history=100):
        self.host = host
        self.port = port
        self.max_history = max_history

        # clientes: nombre -> {'socket': sock, 'addr': (ip,port)}
        self.clientes = {}
        self.lock = threading.Lock()

        # historial del chat general (lista de dicts: {'time','from','msg'})
        self.historial = []

        self.running = True

    def iniciar(self):
        servidor = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        servidor.bind((self.host, self.port))
        servidor.listen(50)
        print(f"[Servidor] escuchando en {self.host}:{self.port}")

        try:
            while self.running:
                conn, addr = servidor.accept()
                hilo = threading.Thread(target=self.manejar_cliente, args=(conn, addr), daemon=True)
                hilo.start()
        except KeyboardInterrupt:
            print("\n[Servidor] detenido por teclado.")
        finally:
            self.running = False
            servidor.close()
            self._cerrar_todo()

    def manejar_cliente(self, conn, addr):
        nombre = None
        try:
            while True:
                data = self._recv_json(conn)
                if data is None:
                    break

                cmd = data.get("cmd")
                if cmd == "login":
                    nombre = (data.get("user") or "").strip()
                    if not nombre:
                        self._send_json(conn, {"status": "error", "msg": "Nombre vacío"})
                        break

                    with self.lock:
                        if nombre in self.clientes:
                            self._send_json(conn, {"status": "error", "msg": "Nombre en uso"})
                            break
                        self.clientes[nombre] = {'socket': conn, 'addr': addr}
                    print(f"[Servidor] {nombre} conectado desde {addr}")

                    # confirmar al cliente
                    self._send_json(conn, {"status": "ok", "msg": f"Bienvenido {nombre}"})

                    # enviar historial del chat general
                    self._send_json(conn, {"cmd": "history", "items": self.historial})

                    # notificar a todos
                    self._broadcast({"cmd": "system", "msg": f"{nombre} se ha unido al chat"}, excluir=nombre)
                    # enviar lista actualizada de usuarios
                    self._broadcast({"cmd": "users", "list": list(self.clientes.keys())})

                elif cmd == "broadcast":
                    texto = data.get("msg", "")
                    if nombre:
                        ts = datetime.now().strftime("%H:%M:%S")
                        registro = {'time': ts, 'from': nombre, 'msg': texto}
                        # almacenar en historial
                        with self.lock:
                            self.historial.append(registro)
                            if len(self.historial) > self.max_history:
                                self.historial.pop(0)
                        # reenviar a todos (incluye al remitente para simplicidad si se desea)
                        self._broadcast({"cmd": "broadcast", "from": nombre, "msg": texto})

                elif cmd == "message":
                    destino = data.get("to")
                    texto = data.get("msg", "")
                    if not nombre:
                        continue
                    with self.lock:
                        if destino in self.clientes:
                            sock_dest = self.clientes[destino]['socket']
                            self._send_json(sock_dest, {"cmd": "private", "from": nombre, "msg": texto})
                            # opcional: confirmar al remitente
                            self._send_json(conn, {"status": "ok", "msg": "Enviado"})
                        else:
                            self._send_json(conn, {"status": "error", "msg": "Usuario no disponible"})

                elif cmd == "users":
                    # cliente solicita lista (aunque el servidor la emite automáticamente en cambios)
                    with self.lock:
                        usuarios = list(self.clientes.keys())
                    self._send_json(conn, {"cmd": "users", "list": usuarios})

                elif cmd == "typing":
                    # indicador de escritura: broadcast a los demás que 'nombre' está escribiendo
                    if nombre:
                        self._broadcast({"cmd": "typing", "user": nombre}, excluir=nombre)

                else:
                    # comando desconocido
                    self._send_json(conn, {"status": "error", "msg": "Comando no reconocido"})

        except (ConnectionResetError, BrokenPipeError):
            pass
        finally:
            # limpieza
            if nombre:
                with self.lock:
                    if nombre in self.clientes:
                        del self.clientes[nombre]
                print(f"[Servidor] {nombre} desconectado")
                # notificar salida
                self._broadcast({"cmd": "system", "msg": f"{nombre} salió del chat"})
                self._broadcast({"cmd": "users", "list": list(self.clientes.keys())})
            try:
                conn.close()
            except:
                pass

    # ---------- utilidades ----------
    def _recv_json(self, sock):
        try:
            raw = sock.recv(4096)
            if not raw:
                return None
            return json.loads(raw.decode('utf-8'))
        except (ConnectionResetError, json.JSONDecodeError):
            return None
        except Exception:
            return None

    def _send_json(self, sock, obj):
        try:
            sock.send(json.dumps(obj).encode('utf-8'))
        except Exception:
            pass

    def _broadcast(self, mensaje, excluir=None):
        with self.lock:
            desconectados = []
            for nombre, info in list(self.clientes.items()):
                if nombre == excluir:
                    continue
                try:
                    info['socket'].send(json.dumps(mensaje).encode('utf-8'))
                except Exception:
                    desconectados.append(nombre)
            # limpiar desconectados
            for n in desconectados:
                if n in self.clientes:
                    del self.clientes[n]

    def _cerrar_todo(self):
        with self.lock:
            for info in list(self.clientes.values()):
                try:
                    info['socket'].close()
                except:
                    pass
            self.clientes.clear()

if __name__ == "__main__":
    servidor = Servidor(host="0.0.0.0", port=5000)
    servidor.iniciar()
