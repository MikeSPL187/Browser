# Инструменты холста дизайна

- `gencss.mjs` генерирует `canvas/vola4-colors.css` из цветов пространств (Material Color
  Utilities, схема M3 2025): светлая, тёмная, OLED и повышенный контраст. Библиотека нужна только
  для макетов и в приложение не попадает. Запуск во временной папке:
  `npm install @material/material-color-utilities@0.4.0`, затем
  `node gencss.mjs > ../canvas/vola4-colors.css`.
- `contrast.py` проверяет контраст пар «текст — фон» в сгенерированном CSS (порог 4,5:1):
  `python3 contrast.py ../canvas/vola4-colors.css`.
- `build_v4.py` собирает доски v4 (`canvas/V4*.dc.html`) вместе с `baikal.svg`:
  `python3 build_v4.py page tabs address anime`.
