; Extra installer steps for Kerf (used by electron-builder).
; Adds Kerf to the "Open with" list for drawings and G-code in File Explorer,
; without replacing the program that already opens them (for example AutoCAD for .dwg).

!macro customInstall
  WriteRegStr HKCU "Software\Classes\Kerf.Drawing" "" "Drawing or toolpath"
  WriteRegStr HKCU "Software\Classes\Kerf.Drawing\DefaultIcon" "" "$INSTDIR\${APP_EXECUTABLE_FILENAME},0"
  WriteRegStr HKCU "Software\Classes\Kerf.Drawing\shell\open\command" "" '"$INSTDIR\${APP_EXECUTABLE_FILENAME}" "%1"'
  WriteRegStr HKCU "Software\Classes\.dxf\OpenWithProgids" "Kerf.Drawing" ""
  WriteRegStr HKCU "Software\Classes\.dwg\OpenWithProgids" "Kerf.Drawing" ""
  WriteRegStr HKCU "Software\Classes\.svg\OpenWithProgids" "Kerf.Drawing" ""
  WriteRegStr HKCU "Software\Classes\.nc\OpenWithProgids" "Kerf.Drawing" ""
  WriteRegStr HKCU "Software\Classes\.gcode\OpenWithProgids" "Kerf.Drawing" ""
  WriteRegStr HKCU "Software\Classes\.gc\OpenWithProgids" "Kerf.Drawing" ""
  WriteRegStr HKCU "Software\Classes\.ngc\OpenWithProgids" "Kerf.Drawing" ""
  WriteRegStr HKCU "Software\Classes\.tap\OpenWithProgids" "Kerf.Drawing" ""
  WriteRegStr HKCU "Software\Classes\.cnc\OpenWithProgids" "Kerf.Drawing" ""
  System::Call 'shell32::SHChangeNotify(i 0x08000000, i 0, p 0, p 0)'
!macroend

!macro customUnInstall
  DeleteRegValue HKCU "Software\Classes\.dxf\OpenWithProgids" "Kerf.Drawing"
  DeleteRegValue HKCU "Software\Classes\.dwg\OpenWithProgids" "Kerf.Drawing"
  DeleteRegValue HKCU "Software\Classes\.svg\OpenWithProgids" "Kerf.Drawing"
  DeleteRegValue HKCU "Software\Classes\.nc\OpenWithProgids" "Kerf.Drawing"
  DeleteRegValue HKCU "Software\Classes\.gcode\OpenWithProgids" "Kerf.Drawing"
  DeleteRegValue HKCU "Software\Classes\.gc\OpenWithProgids" "Kerf.Drawing"
  DeleteRegValue HKCU "Software\Classes\.ngc\OpenWithProgids" "Kerf.Drawing"
  DeleteRegValue HKCU "Software\Classes\.tap\OpenWithProgids" "Kerf.Drawing"
  DeleteRegValue HKCU "Software\Classes\.cnc\OpenWithProgids" "Kerf.Drawing"
  DeleteRegKey HKCU "Software\Classes\Kerf.Drawing"
  System::Call 'shell32::SHChangeNotify(i 0x08000000, i 0, p 0, p 0)'
!macroend
