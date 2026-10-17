# Box

Plugin Paper de box à clés posées sur des blocs, inspiré d'ExcellentCrates.
Compilé contre Paper API 1.21.11, Java 21, sans code NMS : il ne touche qu'à l'API stable de Paper,
pour continuer à fonctionner après la mise à jour vers 26.x.

## Démarrage rapide

1. `/box create legendaire` : crée `plugins/Box/crates/legendaire.yml`.
2. Vise un bloc (coffre, ender chest, beacon...) et fais `/box set legendaire`.
3. `/box edit legendaire` : shift+clic sur des objets de ton inventaire pour les ajouter en récompenses,
   puis règle les poids (clic gauche/droit).
4. `/box give Pseudo legendaire 3` (clés physiques) ou `/box give Pseudo legendaire 3 virtuelle`.

Clic droit sur la box : ouverture (roulette). Accroupi + clic droit : ouverture instantanée.
Clic gauche : aperçu des récompenses et des chances.

## Commandes (`/box`, alias `/caisse`, `/crate`)

| Commande | Rôle |
|---|---|
| `create <id>` / `delete <id>` / `list` | gérer les box |
| `set <box>` / `remove` | poser / retirer une box sur le bloc visé |
| `edit <box>` | éditeur des récompenses |
| `addreward <box> [poids]` | ajouter l'objet en main |
| `preview <box> [joueur]` | aperçu |
| `open <box> [joueur]` | ouverture gratuite |
| `give <joueur\|*> <box> [qté] [virtuelle]` | donner des clés |
| `take <joueur> <box> [qté]` | retirer des clés virtuelles |
| `keys [joueur]` | voir les clés virtuelles |
| `reload` | recharger |

Permissions : `box.open` (tous), `box.keys` (tous), `box.admin` (op). Chaque box peut exiger une
permission en plus (`permission:` dans son fichier).

## Fichiers

- `config.yml` : messages (MiniMessage), menus, options générales.
- `crates/<id>.yml` : une box (nom, hologramme, particules, animation, cooldown, clé, emplacements, récompenses).
  Voir `crates/commune.yml` pour un exemple commenté.
- `keys.yml` : clés virtuelles des joueurs.

Les objets ajoutés en jeu sont stockés au format de Paper (`serializeAsBytes`), qui passe par le
DataFixer de Minecraft : ils sont convertis automatiquement lors des mises à jour du serveur.
