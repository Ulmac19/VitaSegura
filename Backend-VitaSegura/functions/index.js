/**
 * Cloud Functions de VitaSegura.
 *
 * Expone dos funciones callable para el flujo de recuperación de contraseña:
 * - enviarCodigoRecuperacion: genera un código de 6 dígitos (vigencia de 5 min)
 *   y lo envía por correo mediante nodemailer.
 * - cambiarPasswordOlvidada: valida el contexto y actualiza la contraseña del
 *   usuario en Firebase Auth con privilegios de administrador.
 */

require('dotenv').config();
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

// Cliente SMTP de Gmail; las credenciales se leen de variables de entorno
const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
        user: process.env.GMAIL_USER,
        pass: process.env.GMAIL_PASS
    }
});

/**
 * Genera un código de recuperación, lo guarda en la base de datos y lo envía al
 * correo indicado.
 */
exports.enviarCodigoRecuperacion = functions.https.onCall(async (request) => {

    // El payload puede llegar envuelto en 'data' según la versión del SDK
    let email = "";
    if (request && request.data && request.data.email) {
        email = request.data.email;
    } else if (request && request.email) {
        email = request.email;
    }

    if (!email) {
        throw new functions.https.HttpsError("invalid-argument", "No se recibió el correo electrónico desde la app.");
    }
    
    const codigo = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = Date.now() + 5 * 60 * 1000;
    
    try {
        await admin.database().ref(`CodigosRecuperacion/${email.replace(/\./g, "_")}`).set({
            codigo: codigo,
            expiresAt: expiresAt
        });

        const mailOptions = {
            from: "VitaSegura <no-reply@vitasegura.com>",
            to: email,
            subject: "Código de recuperación - VitaSegura",
            html: `
                <div style="font-family: Arial, sans-serif; text-align: center;">
                    <h2 style="color: #23608C;">Recuperación de Contraseña</h2>
                    <p>Has solicitado recuperar tu contraseña en <b>VitaSegura</b>.</p>
                    <p>Tu código de verificación es:</p>
                    <h1 style="color: #23608C; letter-spacing: 5px;">${codigo}</h1>
                    <p>Este código expirará en 5 minutos.</p>
                </div>
            `
        };

        await transporter.sendMail(mailOptions);
        return { success: true, message: "Código enviado correctamente" };

    } catch (error) {
        console.error("Error al enviar correo:", error);
        throw new functions.https.HttpsError("internal", error.message);
    }
});

/**
 * Verifica los datos recibidos y actualiza la contraseña del usuario en Firebase
 * Auth, eliminando después el código de recuperación.
 */
exports.cambiarPasswordOlvidada = functions.https.onCall(async (request) => {
    let email = "";
    let newPassword = "";

    // El payload puede llegar envuelto en 'data' según la versión del SDK
    if (request && request.data) {
        email = request.data.email;
        newPassword = request.data.newPassword;
    } else {
        email = request.email;
        newPassword = request.newPassword;
    }

    if (!email || !newPassword) {
        throw new functions.https.HttpsError("invalid-argument", "Faltan datos para cambiar la contraseña.");
    }

    try {
        // 1. Localiza al usuario por su correo
        const userRecord = await admin.auth().getUserByEmail(email);

        // 2. Actualiza la contraseña con privilegios de administrador
        await admin.auth().updateUser(userRecord.uid, {
            password: newPassword
        });

        // Elimina el código para impedir su reutilización
        await admin.database().ref(`CodigosRecuperacion/${email.replace(/\./g, "_")}`).remove();

        return { success: true, message: "Contraseña actualizada correctamente desde el servidor" };

    } catch (error) {
        console.error("Error al actualizar contraseña:", error);
        throw new functions.https.HttpsError("internal", "No se pudo actualizar la contraseña.");
    }
});