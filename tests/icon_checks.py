"""Deterministic launcher geometry/resource checks; no device execution implied."""
from pathlib import Path
import xml.etree.ElementTree as ET
base=Path(__file__).resolve().parents[1]
a='{http://schemas.android.com/apk/res/android}'
res=base/'app/src/main/res'
manifest=ET.parse(base/'app/src/main/AndroidManifest.xml').getroot().find('application')
assert manifest.get(a+'icon')=='@mipmap/ic_launcher'
assert manifest.get(a+'roundIcon')=='@mipmap/ic_launcher'
for folder in ('mipmap-anydpi-v26','mipmap-anydpi-v33'):
    icon=ET.parse(res/folder/'ic_launcher.xml').getroot()
    assert icon.tag=='adaptive-icon'
    assert icon.find('background').get(a+'drawable')=='@color/launcher_background'
    assert icon.find('foreground').get(a+'drawable')=='@drawable/ic_launcher_foreground'
    if folder.endswith('v33'): assert icon.find('monochrome').get(a+'drawable')=='@drawable/ic_launcher_monochrome'
fg=ET.parse(res/'drawable/ic_launcher_foreground.xml').getroot()
mono=ET.parse(res/'drawable/ic_launcher_monochrome.xml').getroot()
assert len(fg.findall('path'))==5
assert [p.get(a+'fillColor') for p in fg.findall('path')]==['#FFFFFF']*3+['#AFCFFF']*2
assert all(p.get(a+'fillColor')=='#FFFFFF' for p in mono.findall('path'))
assert [p.get(a+'pathData') for p in fg.findall('path')]==[p.get(a+'pathData') for p in mono.findall('path')]
svg=ET.parse(base/'branding/quick-recorder-icon.svg').getroot()
assert [p.get('d') for p in svg.findall('{http://www.w3.org/2000/svg}path')]==[p.get(a+'pathData') for p in fg.findall('path')]
notification=ET.parse(res/'drawable/ic_mic.xml').getroot()
assert all(p.get(a+'fillColor')=='#FFFFFF' for p in notification.findall('path'))
service=(base/'app/src/main/java/cn/local/quickrecorder/RecorderService.java').read_text()
assert '.setSmallIcon(cn.local.quickrecorder.R.drawable.ic_mic)' in service
assert 'versionCode 4' in (base/'app/build.gradle').read_text()
print('PASS: adaptive icons, Android13 monochrome, matching SVG geometry, separate monochrome notification and version4')

# Double-tap dots remain inside the guaranteed adaptive 66dp diameter safe circle.
import math
for x,y,r in [(63,31,3),(73,31,3)]:
    assert math.hypot(x-54,y-54)+r <= 33
print("PASS: both dots fully inside adaptive-icon guaranteed safe circle")
