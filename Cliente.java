import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public class Cliente extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String usuario;
    private String host;
    private int puerto;
    private JList<String> listaUsuarios;
    
    // Componentes de la interfaz
    private JTextArea areaChat;
    private JTextField campoMensaje;
    private DefaultListModel<String> modeloUsuarios;
    private JLabel labelEstado;
    private boolean conectado = true;
    
    public Cliente() {
        pedirDatosConexion();
    
    }
    
    private void pedirDatosConexion() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField campoUsuario = new JTextField();
        JTextField campoHost = new JTextField("localhost");
        JTextField campoPuerto = new JTextField("5000");
        
        panel.add(new JLabel("Usuario:"));
        panel.add(campoUsuario);
        panel.add(new JLabel("Host:"));
        panel.add(campoHost);
        panel.add(new JLabel("Puerto:"));
        panel.add(campoPuerto);
        
        int resultado = JOptionPane.showConfirmDialog(null, panel, "Conectar al chat", 
                                                     JOptionPane.OK_CANCEL_OPTION);
        
        if (resultado == JOptionPane.OK_OPTION) {
            usuario = campoUsuario.getText().trim();
            host = campoHost.getText().trim();
            
            if (usuario.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Debe ingresar un nombre de usuario");
                pedirDatosConexion();
                return;
            }
            
            try {
                puerto = Integer.parseInt(campoPuerto.getText().trim());
                conectarServidor();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Puerto debe ser un número válido");
                pedirDatosConexion();
            }
        } else {
            System.exit(0);
        }
    }
    
    private void conectarServidor() {
        try {
            socket = new Socket(host, puerto);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Enviar login en formato JSON
            Map<String, String> loginData = new HashMap<>();
            loginData.put("cmd", "login");
            loginData.put("user", usuario);
            String loginJson = crearJsonSimple(loginData);
            out.println(loginJson);
            
            // Leer respuesta del servidor
            String respuesta = in.readLine();
            if (respuesta == null) {
                throw new IOException("No response from server");
            }
            
            Map<String, Object> jsonRespuesta = parsearJsonSimple(respuesta);
            
            if ("ok".equals(jsonRespuesta.get("status"))) {
                crearInterfaz();
                new Thread(this::recibirMensajes).start();
            } else {
                String errorMsg = (String) jsonRespuesta.get("msg");
                JOptionPane.showMessageDialog(this, "Error: " + (errorMsg != null ? errorMsg : "Error desconocido"));
                pedirDatosConexion();
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error conectando: " + e.getMessage());
            pedirDatosConexion();
        }
    }
    
    // Método para crear JSON manualmente - CORREGIDO
    private String crearJsonSimple(Map<String, String> datos) {
        StringBuilder json = new StringBuilder("{");
        boolean primero = true;
        
        for (Map.Entry<String, String> entry : datos.entrySet()) {
            if (!primero) {
                json.append(", ");
            }
            json.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            primero = false;
        }
        json.append("}");
        return json.toString();
    }
    
    // Método para parsear JSON manualmente - MEJORADO
    private Map<String, Object> parsearJsonSimple(String jsonStr) {
        Map<String, Object> mapa = new HashMap<>();
        try {
            jsonStr = jsonStr.trim();
            if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
            }
            
            // Manejar parsing de objetos JSON simples
            List<String> pares = new ArrayList<>();
            int depth = 0;
            StringBuilder current = new StringBuilder();
            
            for (char c : jsonStr.toCharArray()) {
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;
                if (c == ',' && depth == 0) {
                    pares.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            pares.add(current.toString().trim());
            
            for (String par : pares) {
                if (par.isEmpty()) continue;
                
                String[] keyValue = par.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                    String value = keyValue[1].trim();
                    
                    // Manejar diferentes tipos de valores
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        mapa.put(key, value.substring(1, value.length() - 1));
                    } else if (value.startsWith("[") && value.endsWith("]")) {
                        mapa.put(key, parsearJsonArraySimple(value));
                    } else if (value.equals("true") || value.equals("false")) {
                        mapa.put(key, Boolean.parseBoolean(value));
                    } else {
                        try {
                            mapa.put(key, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            mapa.put(key, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parseando JSON: " + e.getMessage());
            System.err.println("JSON recibido: " + jsonStr);
        }
        return mapa;
    }
    
    // Método para parsear array JSON simple
    private List<String> parsearJsonArraySimple(String jsonStr) {
        List<String> lista = new ArrayList<>();
        try {
            String contenido = jsonStr.trim();
            if (contenido.startsWith("[")) contenido = contenido.substring(1);
            if (contenido.endsWith("]")) contenido = contenido.substring(0, contenido.length() - 1);
            contenido = contenido.trim();
            
            // Dividir por comas y limpiar comillas
            String[] elementos = contenido.split(",");
            for (String elemento : elementos) {
                String item = elemento.trim().replace("\"", "");
                if (!item.isEmpty()) {
                    lista.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parseando array JSON: " + e.getMessage());
        }
        return lista;
    }
    
    private void crearInterfaz() {
        setTitle("Chat - " + usuario);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                desconectar();
                System.exit(0);
            }
        });
        
        // Panel principal
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);
        
        // Panel derecho (chat)
        JPanel panelChat = new JPanel(new BorderLayout());
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollChat = new JScrollPane(areaChat);
        panelChat.add(scrollChat, BorderLayout.CENTER);
        
        // Panel de entrada de mensaje
        JPanel panelEntrada = new JPanel(new BorderLayout(5, 5));
        panelEntrada.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        campoMensaje = new JTextField();
        campoMensaje.addActionListener(e -> enviarMensaje());
        
        JButton btnEnviar = new JButton("Enviar");
        btnEnviar.addActionListener(e -> enviarMensaje());
        
        // Botón para mensaje privado
    JButton btnPrivado = new JButton("Privado");
    btnPrivado.addActionListener(e -> {
        String usuarioSeleccionado = listaUsuarios.getSelectedValue(); // <-- Ahora funciona
        if (usuarioSeleccionado != null && !usuarioSeleccionado.equals(usuario)) {
            String mensaje = JOptionPane.showInputDialog(this, "Mensaje para " + usuarioSeleccionado + ":");
            if (mensaje != null && !mensaje.trim().isEmpty()) {
                enviarMensajePrivado(usuarioSeleccionado, mensaje.trim());
                String tiempo = new SimpleDateFormat("HH:mm:ss").format(new Date());
                areaChat.append("[" + tiempo + "] [Privado a " + usuarioSeleccionado + "]: " + mensaje + "\n");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario diferente a ti primero");
        }
    });
        
        panelEntrada.add(campoMensaje, BorderLayout.CENTER);
        panelEntrada.add(btnEnviar, BorderLayout.EAST);
        panelEntrada.add(btnPrivado, BorderLayout.WEST);
        panelChat.add(panelEntrada, BorderLayout.SOUTH);
        
        // Panel izquierdo (usuarios)
        JPanel panelUsuarios = new JPanel(new BorderLayout());
    panelUsuarios.setBorder(BorderFactory.createTitledBorder("Usuarios Conectados"));
    modeloUsuarios = new DefaultListModel<>();
    listaUsuarios = new JList<>(modeloUsuarios); // <-- Aquí se declara correctamente
    listaUsuarios.setFont(new Font("Arial", Font.PLAIN, 14));
    panelUsuarios.add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);
        
        // Barra de estado
        labelEstado = new JLabel(" Conectado a " + host + ":" + puerto);
        labelEstado.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Ensamblar interfaz
        splitPane.setLeftComponent(panelChat);
        splitPane.setRightComponent(panelUsuarios);
        add(splitPane, BorderLayout.CENTER);
        add(labelEstado, BorderLayout.SOUTH);
        
        setVisible(true);
        campoMensaje.requestFocus();
        
        // Solicitar lista de usuarios
        solicitarUsuarios();
    }
    
    private void enviarMensaje() {
        String mensaje = campoMensaje.getText().trim();
        if (mensaje.isEmpty()) return;
        
        try {
            Map<String, String> datos = new HashMap<>();
            datos.put("cmd", "broadcast");
            datos.put("msg", mensaje);
            out.println(crearJsonSimple(datos));
            
            // Mostrar mensaje localmente
            String tiempo = new SimpleDateFormat("HH:mm:ss").format(new Date());
            areaChat.append("[" + tiempo + "] Tú: " + mensaje + "\n");
            campoMensaje.setText("");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error enviando mensaje: " + e.getMessage());
        }
    }
    
    private void enviarMensajePrivado(String destinatario, String mensaje) {
        try {
            Map<String, String> datos = new HashMap<>();
            datos.put("cmd", "message");
            datos.put("to", destinatario);
            datos.put("msg", mensaje);
            out.println(crearJsonSimple(datos));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error enviando mensaje privado: " + e.getMessage());
        }
    }
    
    private void solicitarUsuarios() {
        try {
            Map<String, String> datos = new HashMap<>();
            datos.put("cmd", "users");
            out.println(crearJsonSimple(datos));
        } catch (Exception e) {
            System.err.println("Error solicitando usuarios: " + e.getMessage());
        }
    }
    
    private void recibirMensajes() {
        try {
            String linea;
            while (conectado && (linea = in.readLine()) != null) {
                procesarMensaje(linea);
            }
        } catch (Exception e) {
            if (conectado) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Conexión perdida: " + e.getMessage());
                    System.exit(0);
                });
            }
        }
    }
    
    private void procesarMensaje(String mensajeJson) {
        SwingUtilities.invokeLater(() -> {
            try {
                Map<String, Object> mensaje = parsearJsonSimple(mensajeJson);
                String comando = (String) mensaje.get("cmd");
                
                if (comando == null) {
                    // Verificar si es un mensaje de estado
                    String status = (String) mensaje.get("status");
                    if ("error".equals(status)) {
                        String errorMsg = (String) mensaje.get("msg");
                        areaChat.append("Error: " + errorMsg + "\n");
                    }
                    return;
                }
                
                switch (comando) {
                    case "broadcast":
                        String remitente = (String) mensaje.get("from");
                        String texto = (String) mensaje.get("msg");
                        String tiempo = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        areaChat.append("[" + tiempo + "] " + remitente + ": " + texto + "\n");
                        break;
                        
                    case "users":
                        Object listaObj = mensaje.get("list");
                        if (listaObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> usuarios = (List<String>) listaObj;
                            modeloUsuarios.clear();
                            for (String user : usuarios) {
                                if (!user.equals(usuario)) {
                                    modeloUsuarios.addElement(user);
                                }
                            }
                        }
                        break;
                        
                    case "system":
                        texto = (String) mensaje.get("msg");
                        areaChat.append("Sistema: " + texto + "\n");
                        break;
                        
                    case "history":
                        Object itemsObj = mensaje.get("items");
                        if (itemsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, String>> items = (List<Map<String, String>>) itemsObj;
                            areaChat.append("--- Historial de mensajes ---\n");
                            for (Map<String, String> item : items) {
                                String time = item.get("time");
                                String from = item.get("from");
                                String msg = item.get("msg");
                                areaChat.append("[" + time + "] " + from + ": " + msg + "\n");
                            }
                        }
                        break;
                        
                    case "private":
                        remitente = (String) mensaje.get("from");
                        texto = (String) mensaje.get("msg");
                        tiempo = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        areaChat.append("[" + tiempo + "] [PRIVADO de " + remitente + "]: " + texto + "\n");
                        break;
                        
                    case "typing":
                        String userTyping = (String) mensaje.get("user");
                        if (userTyping != null && !userTyping.equals(usuario)) {
                            labelEstado.setText(" " + userTyping + " está escribiendo...");
                            // Limpiar después de 3 segundos
                            Timer timer = new Timer(3000, e -> labelEstado.setText(" Conectado a " + host + ":" + puerto));
                            timer.setRepeats(false);
                            timer.start();
                        }
                        break;
                }
                
                // Auto-scroll al final
                areaChat.setCaretPosition(areaChat.getDocument().getLength());
                
            } catch (Exception e) {
                System.err.println("Error procesando mensaje: " + e.getMessage());
                System.err.println("Mensaje recibido: " + mensajeJson);
            }
        });
    }
    
    private void desconectar() {
        conectado = false;
        try {
            // Enviar mensaje de desconexión
            Map<String, String> datos = new HashMap<>();
            datos.put("cmd", "logout");
            datos.put("user", usuario);
            out.println(crearJsonSimple(datos));
        } catch (Exception e) {
            // Ignorar errores en desconexión
        }
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando conexión: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente());
    }
}