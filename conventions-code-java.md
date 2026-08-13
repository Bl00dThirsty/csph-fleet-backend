# Conventions de code Java — Résumé

> Basé sur les Java Code Conventions de Sun Microsystems (traduction française).

## 1. Noms de fichiers
- Code source Java : `.java` — Bytecode : `.class`
- `GNUmakefile` pour gnumake, `README` pour résumer un répertoire

## 2. Organisation d'un fichier
- Un fichier = une seule classe/interface **publique** (la classe publique en premier)
- Éviter les fichiers de plus de 2000 lignes
- Structure : commentaire d'en-tête → `package`/`import` → déclarations de classe

**Commentaire d'en-tête** (style C) : nom de la classe, version, date, copyright.

**Ordre dans une classe/interface :**
1. Javadoc
2. Déclaration `class`/`interface`
3. Commentaire d'implémentation (si besoin)
4. Variables de classe (`static`) : public → protected → package → private
5. Variables d'instance : public → protected → package → private
6. Constructeurs (constructeur par défaut d'abord)
7. Méthodes, groupées par **fonctionnalité** (pas par portée)

## 3. Indentation
- 4 espaces par niveau (tabulation = 8 espaces si utilisée)
- Lignes ≤ 80 caractères (≤ 70 dans la documentation)
- **Lignes brisées** : couper après une virgule, avant un opérateur, privilégier les coupures de haut niveau ; sinon indenter de 8 espaces
- Pour les `if` multi-lignes, préférer une indentation de 8 espaces (plus lisible que l'alignement conventionnel)

## 4. Commentaires
- **Implémentation** : `/* ... */` et `//` (4 styles : bloc, ligne, inséré, fin de ligne)
- **Javadoc** : `/** ... */`, un commentaire par classe/interface/membre, placé juste avant la déclaration
- Un commentaire de bloc doit être précédé d'une ligne blanche
- Éviter les commentaires redondants ou risquant de devenir obsolètes
- Ne jamais mettre de javadoc à l'intérieur du corps d'une méthode

### Exemples adoptés dans ce projet

**Javadoc pour les classes** (avec `@author`, `@version`, etc.) :

```java
/**
 * Ecran de chargement de la tournee : affiche les informations de la
 * tournee ainsi que les tags scannes.
 *
 * @author  John MANGA | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   20.07.2026
 */
public class EcranChargementTournee {
    ...
}
```

**Commentaire simple pour les méthodes** (bloc `/* ... */`, sans balises `@param`) :

```java
/*
 * Affiche l'ecran de chargement de la tournee : donnees de la tournee,
 * points de livraison, tags scannes, et etat du scan en cours.
 */
public void renderTourLoadingScreen(int tourId, Tour tour,
        List<PointDeLivraison> points, List<TagScanne> scannedTags,
        boolean isScanning, Consumer<Boolean> onScanToggle,
        Runnable onClearScans, Consumer<String> onRemoveTag,
        Runnable onForceStartTour, Runnable onBackClick) {
    ...
}
```

> Le javadoc (`/** */`) est réservé au niveau classe/interface pour la doc publique générée ; un commentaire de bloc classique (`/* */`) suffit pour une méthode, sans avoir à documenter chaque paramètre.

## 5. Déclarations
- Une seule déclaration par ligne (favorise les commentaires)
- Ne jamais mélanger différents types sur une même ligne
- Initialiser les variables locales dès leur déclaration si possible
- Déclarer les variables en début de bloc (exception : index de boucle `for`)
- Éviter de masquer une déclaration de niveau supérieur

**Déclarations de classes/interfaces :**
- Pas d'espace entre le nom de méthode et `(`
- `{` en fin de ligne de déclaration
- `}` sur une nouvelle ligne alignée avec le début, sauf corps vide (`{}` collé)

## 6. Instructions
- Une seule instruction par ligne
- Toujours utiliser des accolades `{}`, même pour un bloc d'une instruction
- `return` sans parenthèses sauf si cela améliore la lisibilité
- Formes standard pour `if/else`, `for`, `while`, `do-while`, `switch`, `try-catch(-finally)`
- `switch` : toujours prévoir un `default`, commenter les cas sans `break` (`/* passe au suivant */`)

## 7. Blancs
**Lignes blanches**
- 2 lignes vides : entre sections d'un fichier, entre classes/interfaces
- 1 ligne vide : entre méthodes, entre déclarations et premières instructions, avant un commentaire de bloc/ligne, entre sections logiques

**Espaces**
- Un espace après un mot-clé suivi de `(` (ex. `while (true)`), mais **pas** entre un nom de méthode et `(`
- Après chaque virgule dans une liste d'arguments
- Autour des opérateurs binaires (pas pour les opérateurs unaires `-`, `++`, `--`)
- Après un cast : `(byte) unNombre`

## 8. Conventions de nommage
| Type | Exemple | Règle |
|---|---|---|
| Packages | `com.sun.eng` | Préfixe en minuscules ASCII (domaine), puis convention interne |
| Classes/interfaces | `ListeSimple` | CamelCase, première lettre majuscule, mots entiers |
| Méthodes | `coursPlusVite()` | Verbe conjugué, camelCase (1ère lettre minuscule) |
| Variables | `maLargeur` | camelCase, pas de `_` ni `$` en début, noms courts mais parlants |
| Constantes | `INDICE_MAX` | MAJUSCULES avec `_` entre les mots |

## 9. Techniques de programmation
- Ne pas rendre une variable publique sans bonne raison (sauf classes "struct-like")
- Accéder aux membres `static` via le **nom de la classe**, pas via une instance
- Pas de constantes numériques "en dur" (sauf -1, 0, 1 pour les compteurs)
- Éviter les affectations multiples/imbriquées sur une même ligne
- Toujours parenthéser les expressions mêlant plusieurs opérateurs
- Simplifier les retours conditionnels : `return expressionBooleenne;` plutôt qu'un `if/else` avec `true`/`false`
- Commentaires spéciaux : `XXX` (bogué mais fonctionnel), `FIXME` (bogué et non fonctionnel)

## Annexes
- **Annexe A** : exemple complet de fichier Java bien formaté
- **Annexe B** : licence de diffusion Sun Microsystems (BSD-like)
- **Références** : *The Java Language Specification* (Gosling, Joy, Steele), *Java Code Conventions* (Hommel, 1999)
