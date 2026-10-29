from pathlib import Path
import hashlib
base = Path(__file__).resolve().parents[1]
EXPECTED = {'Status': '94bc4cc5e0587b9cadc0afaf7e3361baaaa0cfb3015d53eed2008edaa814bcfc', 'RecordingProvider': 'e44c7385940836bf5a6aeb81ea1a5709a4d1521438140f2067e6722a223667d9', 'DoublePress': '18cfa30bd16896e05ac6f32b2c87b6e792bf24657c12f348dd653818d58ace78'}
for name, expected in EXPECTED.items():
    path = base / 'app/src/main/java/cn/local/quickrecorder' / (name+'.java')
    assert hashlib.sha256(path.read_bytes()).hexdigest() == expected, name
print('PASS: status, provider and gesture unchanged from 0.1')

def lum(h):
    v=[int(h[i:i+2],16)/255 for i in (1,3,5)]
    r,g,b=[c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4 for c in v]
    return .2126*r+.7152*g+.0722*b
pairs=[('#202C3F','#FFFFFF'),('#617087','#FFFFFF'),('#617087','#F3F5F9'),('#FFFFFF','#4564A5'),('#4564A5','#EAF0FA'),('#EFF3FA','#1B2432'),('#B1BED0','#1B2432'),('#15253D','#AFC8FF'),('#AFC8FF','#26364D'),('#FFFFFF','#AE393F'),('#3D171B','#FFB7B4')]
for fg,bg in pairs:
    a,b=sorted([lum(fg),lum(bg)])
    assert (b+.05)/(a+.05)>=4.5,(fg,bg)
print('PASS: all 11 main body/secondary/button text color pairs meet 4.5:1 contrast')
