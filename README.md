# DroidVault — ক্যালকুলেটর ভল্ট অ্যাপ

একটা সম্পূর্ণ অফলাইন Android প্রজেক্ট (Kotlin, Android Studio)। কাজ করে সাধারণ ক্যালকুলেটর হিসেবে, কিন্তু ৫ ডিজিটের পাসওয়ার্ড টাইপ করে `=` চাপলে একটা লুকানো ভল্ট খোলে যেখানে আপনি বেছে নেওয়া অ্যাপগুলোর হোমস্ক্রিন আইকন সত্যিকারভাবে লুকিয়ে ফেলতে পারবেন।

**কোনো ইন্টারনেট পারমিশন নেই, কোনো ডেটা বাইরে যায় না। PIN ও লুকানো অ্যাপের লিস্ট শুধু আপনার ফোনেই (SharedPreferences) থাকে, SHA-256 হ্যাশ করা অবস্থায়।**

---

## ধাপ ১ — Android Studio দিয়ে বিল্ড করুন

1. Android Studio-তে এই `DroidVault` ফোল্ডারটা "Open" করুন।
2. Gradle sync শেষ হওয়া পর্যন্ত অপেক্ষা করুন।
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)` — এতে একটা APK তৈরি হবে (`app/build/outputs/apk/debug/app-debug.apk`)।
4. অথবা সরাসরি আপনার ফোন USB দিয়ে কানেক্ট করে `Run ▶` চাপলেই ইনস্টল হয়ে যাবে।

## ধাপ ২ — Device Owner সেট করা (একবারই, বাধ্যতামূলক)

Android নিরাপত্তার কারণে সাধারণ কোনো অ্যাপকে অন্য অ্যাপের আইকন হাইড করতে দেয় না — শুধুমাত্র **Device Owner** অ্যাপ এটা পারে। Device Owner সেট করতে হলে ফোনে **কোনো Google/অন্য অ্যাকাউন্ট লগইন থাকা যাবে না** (নতুন ফোন বা factory reset করা ফোনে সবচেয়ে সহজ)।

### প্রস্তুতি
- কম্পিউটারে [ADB (Android Platform Tools)](https://developer.android.com/tools/releases/platform-tools) ইনস্টল করুন।
- ফোনে **Settings > About phone > Build number** এ ৭ বার ট্যাপ করে **Developer options** চালু করুন।
- **Developer options > USB debugging** অন করুন।
- ফোনে **কোনো Google অ্যাকাউন্ট অ্যাড করা না থাকলে ভালো**। যদি থাকে, প্রথমে সব অ্যাকাউন্ট রিমুভ করুন (Settings > Accounts) অথবা factory reset দিন।

### কমান্ড
```
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell dpm set-device-owner com.faysal.vaultcalc/.VaultAdminReceiver
```
সফল হলে এরকম মেসেজ দেখাবে: `Success: Device owner set to package com.faysal.vaultcalc`

এখন অ্যাপটা Device Owner — তাই ভল্ট থেকে যেকোনো অ্যাপ সত্যিকারভাবে হোমস্ক্রিন থেকে হাইড/আনহাইড করতে পারবে।

---

## ব্যবহার

1. প্রথমবার খুললে ৫ ডিজিটের পাসওয়ার্ড সেট করতে বলবে।
2. স্বাভাবিক ক্যালকুলেটর হিসেবে ব্যবহার করুন — যোগ, বিয়োগ, গুণ, ভাগ সব কাজ করবে।
3. ভল্টে ঢুকতে: **কোনো যোগ/বিয়োগ/গুণ/ভাগ চাপা ছাড়া** সরাসরি ৫ ডিজিটের পাসওয়ার্ড টাইপ করে `=` চাপুন।
4. ভল্টের ভেতরে `+ অ্যাপ হাইড করুন` চেপে ইনস্টল করা অ্যাপের লিস্ট থেকে যেটা হাইড করতে চান সেটায় ট্যাপ করুন — সাথে সাথে হোমস্ক্রিন/অ্যাপ ড্রয়ার থেকে আইকন উধাও হয়ে যাবে।
5. আনহাইড করতে ভল্টে ঢুকে সেই অ্যাপের পাশের "আনহাইড" বাটনে চাপুন।

## জরুরি সতর্কতা

- **হাইড করা অ্যাপ চলবে না।** `setApplicationHidden` শুধু আইকনই লুকায় না, অ্যাপটাকে সাময়িকভাবে বন্ধও রাখে (নোটিফিকেশন, ব্যাকগ্রাউন্ড সার্ভিস সহ)। আনহাইড করলে আগের মতোই আবার চলবে, কোনো ডেটা হারাবে না।
- পাসওয়ার্ড ভুলে গেলে অ্যাপ আনইনস্টল/রিসেট করে আবার পুরো প্রসেস (Device Owner সহ) করতে হবে। তাই পাসওয়ার্ডটা মনে রাখুন।
- এই অ্যাপ Play Store-এ আপলোড করার উপযোগী না — এটা শুধু ব্যক্তিগত ব্যবহারের জন্য, নিজের ফোনে sideload করে চালানোর জন্য বানানো।
- `applicationId` (`com.faysal.vaultcalc`) বা অ্যাপের নাম/আইকন Android Studio-তে গিয়ে সহজেই বদলে নিতে পারবেন যেন এটা আরও বেশি সাধারণ ক্যালকুলেটরের মতো দেখায়।

## Device Owner বাতিল করতে চাইলে
```
adb shell dpm remove-active-admin com.faysal.vaultcalc/.VaultAdminReceiver
```

---

## ⚠️ Troubleshooting: "Device Owner সেট করা নেই" দেখাচ্ছে / হাইড কাজ করছে না

ভল্ট স্ক্রিনে এখন উপরে একটা স্ট্যাটাস লাইন দেখাবে — লাল দেখালে বুঝবেন Device Owner আসলে সেট হয়নি।

**সবচেয়ে সাধারণ কারণ:** Settings > Security > **Device Admin Apps** এ গিয়ে টগলটা হাতে ON করে দিলে সেটা শুধু সাধারণ "Device Admin" হয় — এটা **Device Owner এর মতো না**। এই দুটো আলাদা জিনিস। শুধু হাতে টগল ON করলে হাইড কখনো কাজ করবে না, `setApplicationHidden` কল করার permission শুধু আসল Device Owner-এর থাকে।

**সমাধান, এই ক্রমে করুন:**

1. প্রথমে Settings > Security/Apps > Device Admin Apps এ গিয়ে অ্যাপটার টগল **OFF** করুন (হাতে ON করা থাকলে)।
2. Settings > Accounts এ গিয়ে ফোনে যত অ্যাকাউন্ট (Google সহ) আছে সব রিমুভ করুন। এটা বাধ্যতামূলক — একটা অ্যাকাউন্টও থাকলে পরের কমান্ড fail করবে।
3. টার্মিনালে রান করুন:
   ```
   adb shell dpm set-device-owner com.faysal.vaultcalc/.VaultAdminReceiver
   ```
4. আউটপুট খেয়াল করুন:
   - `Success: Device owner set to package com.faysal.vaultcalc` → ঠিকমতো হয়েছে।
   - `Not allowed to set the device owner because there are already some accounts on the device.` → ধাপ ২ আবার করুন, সব অ্যাকাউন্ট মুছুন।
   - `Trying to set the device owner, but device owner is already set.` → আগে কেউ (বা এই অ্যাপ নিজেই আগে) device owner হয়ে বসে আছে। যাচাই করতে: `adb shell dumpsys device_policy | grep -i "device owner"`। দরকার হলে সেই আগের অ্যাপে `dpm remove-active-admin` দিয়ে সরিয়ে আবার ট্রাই করুন।
5. কমান্ড সফল হওয়ার পর অ্যাপ খুলে ভল্টে ঢুকে স্ট্যাটাস লাইনটা সবুজ দেখাচ্ছে কিনা চেক করুন — সবুজ মানে এখন হাইড/আনহাইড কাজ করবে।
