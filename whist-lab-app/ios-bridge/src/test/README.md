# Testele punții iOS

## Pe scurt

Acest director verifică adaptarea dintre motorul comun și interfața nativă iOS. Testele rulează pe JVM; ele nu califică build-ul Xcode, simulatorul sau distribuția către dispozitive.

## Contractul verificat

- Proiecția stării publice păstrează ascunse mâinile adversarilor și expune doar acțiunile legale ale jucătorului.
- Traducerea dintre modelele comune și reprezentarea punții păstrează regulile, licitațiile și scorurile.
- Cazurile-limită refuză intrările invalide fără să schimbe starea jocului.

## Rulare

Din directorul `whist-lab-app`, executați:

```powershell
./gradlew :ios-bridge:test --no-daemon
```

Workflow-ul CI iOS păstrat poate rula separat verificări ale motorului comun și ale punții. Acesta nu califică aplicația iOS, suportul simulatorului, semnarea sau distribuția; platforma rămâne în așteptare.

[Strategia comună de testare](../../../../docs/testing/README.md)
