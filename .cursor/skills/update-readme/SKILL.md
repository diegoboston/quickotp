---
name: update-readme
description: >-
  Keep README.md in sync with user-facing app changes. Use whenever you add,
  remove, or change features, UI, permissions, build steps, project layout,
  or agent skills in this repo.
---

# Update README

After **every** task that changes what the app does or how it is built, update `README.md` before marking the work complete.

## When to update

Update the README when you change any of:

- User-visible features or behaviour (screens, controls, flows)
- Permissions, storage paths, or in-app update behaviour
- Project layout (new packages, skills, CI)
- Build, test, or install instructions
- ASCII wireframes in the **Screens** section that no longer match the UI

Do **not** skip this for "small" UI tweaks — banners, snackbars, and new toolbar icons belong in the README too.

## What to edit

1. **Features sections** — add or revise the bullet that best matches the change.
2. **Screens** — update ASCII sketches when layout or flows change.
3. **Project layout tree** — if files or folders were added or renamed.
4. **Cursor agent skills** — if `.cursor/skills/` changed, update the skills table.

Keep prose in the same voice as the existing README: complete sentences, concrete behaviour, no changelog tone.

## Pair with other skills

| Change | Run before README |
| ------ | ----------------- |
| Any file under `app/` or Gradle/Android config | **rebuild-app** |
