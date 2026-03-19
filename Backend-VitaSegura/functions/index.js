require('dotenv').config();
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
        
        user: process.env.GMAIL_USER, 
        pass: process.env.GMAIL_PASS
    }
});

// En las versiones más nuevas de Firebase, el parámetro llega envuelto de forma distinta
exports.enviarCodigoRecuperacion = functions.https.onCall(async (request) => {
    
    // Extraemos el correo electrónico del request, considerando las diferentes formas en que puede llegar
    let email = "";
    if (request && request.data && request.data.email) {
        email = request.data.email;
    } else if (request && request.email) {
        email = request.email;
    }

    // Validamos que se haya recibido el correo electrónico
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

exports.cambiarPasswordOlvidada = functions.https.onCall(async (request) => {
    let email = "";
    let newPassword = "";
    
    // Extraemos los datos enviados desde Android
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
        // 1. Buscamos al usuario por su correo
        const userRecord = await admin.auth().getUserByEmail(email);
        
        // 2. Le forzamos el cambio de contraseña usando sus privilegios de administrador
        await admin.auth().updateUser(userRecord.uid, {
            password: newPassword
        });

        // Borrar el código de la base de datos para que no se re-use
        await admin.database().ref(`CodigosRecuperacion/${email.replace(/\./g, "_")}`).remove();

        return { success: true, message: "Contraseña actualizada correctamente desde el servidor" };

    } catch (error) {
        console.error("Error al actualizar contraseña:", error);
        throw new functions.https.HttpsError("internal", "No se pudo actualizar la contraseña.");
    }
});