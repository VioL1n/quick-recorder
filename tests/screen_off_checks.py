"""Source invariants for removal of the optional wake-screen path in 0.3."""
from pathlib import Path
import re
base = Path(__file__).resolve().parents[1]
main = base / 'app/src/main'
java = main / 'java/cn/local/quickrecorder'
all_source = '\n'.join(p.read_text() for p in main.rglob('*') if p.is_file())
for forbidden in ('RecordActivity', 'setTurnScreenOn', 'FLAG_TURN_SCREEN_ON', 'setShowWhenLocked', 'FLAG_SHOW_WHEN_LOCKED', '亮屏'):
    assert forbidden not in all_source, forbidden
# The only remaining old preference references delete it; never read or write a mode.
lines = [line for line in all_source.splitlines() if 'wakeMode' in line]
assert len(lines) == 2 and all('.remove("wakeMode")' in line for line in lines)
keys = (java / 'VolumeKeyService.java').read_text()
assert 'startActivity(' not in keys
assert re.search(r'else if \(!Status.starting\)\s*\{\s*RecorderService.requestStart\(this\);\s*\}', keys)
assert 'stopService(new Intent(this, RecorderService.class))' in keys
assert 'return false;' in keys
recorder = (java / 'RecorderService.java').read_text()
assert 'new Intent(this,MainActivity.class)' in recorder
launcher = (java / 'MainActivity.java').read_text()
assert 'RecorderService.requestStart(this)' in launcher
assert 'versionCode 3' in (base / 'app/build.gradle').read_text()
print('PASS: no wake-screen Activity/API/branch; legacy preference only removed; direct shortcut and manual recording retained')
