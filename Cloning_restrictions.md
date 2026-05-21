# CLONING RESTRICTIONS

⚠️ **AUTHORIZED CLONING ONLY** ⚠️

This repository requires a **Uniblox Corporation Certificate** (`uc.md`) to clone and build.

## Who Can Clone?

✅ **Authorized** if you have:
- Valid `uc.md` certificate in project root
- Active Uniblox Corporation team membership
- Valid certificate ID (UC-UNIBLOX-XXXX-XXX)

❌ **NOT Authorized** if:
- Missing `uc.md` file
- Expired certificate
- Unauthorized distribution attempt
- Hacked/modified clone

## Build Requirements

Before building in AIDE:

1. **Ensure `uc.md` exists** in project root
   ```
   ls uc.md
   ```

2. **Verify certificate content**
   - Contains Uniblox license terms
   - Includes verification token
   - Has valid Certificate ID

3. **Build will fail without it**
   - App won't initialize
   - Security validation triggers
   - Report sent to Uniblox Security

## What Happens If You Clone Without Authorization?

🚨 **Automatic Detection:**
- Missing `uc.md` = app won't run
- Modified certificate = security alert
- Unauthorized distribution = logged and reported

## For Team Members

If you're authorized:
1. Clone normally
2. Ensure `uc.md` is in root directory
3. Build with AIDE
4. App validates certificate on startup

## Questions?

Contact: **talosigsanjoreyvien@gmail.com**

---

*Protecting Uniblox Innovation* 🔐
