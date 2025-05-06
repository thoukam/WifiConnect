
# 📷 Application Android — Contrôle de caméras 360° compatibles API OSC
Cette application Android permet de piloter à distance des caméras 360° compatibles avec le protocole **OSC (Open Spherical Camera)**.
Elle propose des fonctions de prise de vue, d’enregistrement vidéo, de changement de mode, de synchronisation de l’horloge interne, de visualisation des fichiers médias et de surveillance de l’état de la caméra.

---

## ✅ Caméras compatibles

Fonctionne avec **toutes les caméras 360° compatibles avec l'API OSC**, comme :

- **Ricoh Theta Z1**
- **Ricoh Theta V**
- **Ricoh Theta SC2**
- **Insta360 Pro 2** *(via firmware compatible OSC)*
- **Labpano Pilot One / Era**
- **GoPro Max / Fusion** *(via passerelle compatible OSC)*

---

## 🎯 Fonctionnalités principales

| Fonction                        | Description |
|----------------------------------|-------------|
| 📶 Connexion WiFi directe        | Connexion à la caméra via SSID et mot de passe |
| 📸 Prise de photo                | `camera.takePicture` |
| 🎥 Démarrage/Arrêt vidéo         | `camera.startCapture` / `camera.stopCapture` |
| 🔁 Changement de mode            | Bascule entre `image` et `video` via `camera.setOptions` |
| 🕒 Synchronisation horloge       | Mise à jour via `camera.setOptions` avec `dateTimeZone` |
| 🗂️ Liste des fichiers médias     | Lecture via `camera.listFiles` |
| 🖼️ Galerie intégrée              | Affichage des photos/vidéos en grille avec preview |
| 🔋 Monitoring batterie           | Actualisation toutes les secondes, alerte si <30% |
| ⏱️ Affichage uptime              | Temps de fonctionnement de la caméra visible |

---

## 📂 Architecture du projet

```
📦 app/
 ┣ 📁 activities/
 ┃ ┣ MainActivity.kt              → Connexion WiFi à la caméra
 ┃ ┣ CameraControlActivity.kt     → Contrôle OSC, synchronisation, monitoring
 ┃ ┣ FilePreviewActivity.kt       → Lecture d'image ou vidéo
 ┣ 📁 adapters/
 ┃ ┗ GalleryAdapter.kt            → Galerie RecyclerView
 ┣ 📁 res/layout/
 ┃ ┣ activity_camera_control.xml
 ┃ ┗ gallery_item.xml
```

---

## ⚙️ Permissions Android nécessaires

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<uses-permission android:name="android.permission.VIBRATE" />
```

---

## 🚀 Installation & usage

1. Connecter votre smartphone au WiFi de la caméra 360° (ex. "THETA_XXXX")
2. Ouvrir l'application
3. Renseigner le SSID et mot de passe, puis se connecter
4. Accéder à l'écran de contrôle pour :
   - Prendre une photo
   - Lancer/arrêter un enregistrement
   - Changer de mode (image/vidéo)
   - Synchroniser l’horloge à l’heure UTC
   - Visualiser les fichiers enregistrés
---

## 💡 Exemple de commande OSC : synchronisation UTC

```json
{
  "name": "camera.setOptions",
  "parameters": {
    "options": {
      "dateTimeZone": "2025:04:30 14:00:00+00:00"
    }
  }
}
```

---

## 👤 Auteur

- **Développement** : Thoukam thotchum yves
- **Stage** : Dimorph360 (2025)
- **Encadrants** : A. Billault (ATGT),

---

## 🔗 Références

- [Open Spherical Camera API (OSC)](https://developers.google.com/streetview/open-spherical-camera)
- [API officielle Ricoh Theta](https://github.com/ricohapi/theta-api-specs)
