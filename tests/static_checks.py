from pathlib import Path
import xml.etree.ElementTree as ET
base = Path(__file__).resolve().parents[1]
a = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(base/'app/src/main/AndroidManifest.xml').getroot()
permissions = {x.get(a+'name') for x in manifest.findall('uses-permission')}
assert permissions == {
 'android.permission.RECORD_AUDIO','android.permission.POST_NOTIFICATIONS',
 'android.permission.FOREGROUND_SERVICE','android.permission.FOREGROUND_SERVICE_MICROPHONE',
 'android.permission.VIBRATE'}
app = manifest.find('application')
assert app.get(a+'allowBackup') == 'false'
assert {x.get(a+'name'):x.get(a+'exported') for x in app.findall('activity')} == {'.MainActivity':'true'}
services = {x.get(a+'name'):x for x in app.findall('service')}
assert services['.RecorderService'].get(a+'foregroundServiceType') == 'microphone'
assert services['.RecorderService'].get(a+'exported') == 'false'
assert services['.VolumeKeyService'].get(a+'permission') == 'android.permission.BIND_ACCESSIBILITY_SERVICE'
cfg = ET.parse(base/'app/src/main/res/xml/accessibility.xml').getroot()
assert cfg.get(a+'canRetrieveWindowContent') == 'false'
assert cfg.get(a+'canRequestFilterKeyEvents') == 'true'
assert cfg.get(a+'accessibilityEventTypes') is None
provider = app.find('provider')
assert provider.get(a+'exported') == 'false' and provider.get(a+'grantUriPermissions') == 'true'
print('PASS: permissions, private recording/provider, no network/backup, microphone FGS, key-only accessibility')
