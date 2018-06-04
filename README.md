# IKNOWU: MODE D’EMPLOI

## INSTALLATION DU SYSTEME SUR UN ORDINATEUR
1.	Installer Python 3.6 
2.	Télécharger le repository iKnowU depuis Github et extraire le dossier « iKnowU_Server » dans le parcours désiré.
3.	Dans le dossier « iKnowU_Server/soft_install », executer le bon programme pour installer les librairies nécéssaires à l’execution de iKnowU :
       - Windows : double click sur “windows_install.bat”
       - Linux ou OSX: ouvrir une fênetre de terminal dans le dossier, et écrire « bash ./linux_osx_install.sh »
4.	Essayer à executer une démo pour voir si tout a été installé correctement

## ENREGISTREMENT DE VISAGES

Pour permettre au système de reconnaître les visages vus par les smart glasses, il faut d’abord enregistrer des visages. Pour ce faire, il faut prendre des photos des visages des personnes qu’on veut enregistrer (tests faits avec 5 photos/personne, à une distance de 1, 1.5, 2, 2.5, 3 mètres de distance). Ensuite, sur l’ordinateur, procéder comme suit :
1.	Dans le dossier « iKnowU_Server/training_images », créer un dossier pour chaque personne qu’on veut enregistrer, et nommer le dossier avec le nom de la personne (ex. « iKnowU_Server/training_images/Pascal »).
2.	Copier les photos prises en précedence dans les dossiers nommés corréspondants (ex. copier les photos de Pascal dans « iKnowU_Server/training_images/Pascal »)
3.	Une fois terminé, ouvrir une fenêtre de terminal (invite de commandes en Windows), naviguer au dossier iKnowU_Server, et executer la commande suivante:
```
python ./train_reco.py
```
4.	Enregistrement de visages complété

## DEMO

Pour executer une démo, après avoir connecté les lunettes et l’ordinateur au même réseau, il faut lancer deux applicatifs:
1.	Sur les smart glasses: Allumer les smart glasses > Appuyer sur « Google » en haut à gauche > Écrire « sample » dans le  champ de recherche > Appuyer 2 fois sur la touche « A | Blue » sur la commande des lunettes > Lancer « Sample Camera Preview » 
2.	Sur l’ordinateur :
       a. Si nécéssaire, changer l’adresse IP dans le fichier « iKnowU_Server/facereco.py », en utilisant un editeur de texte
       b. Ouvrir un terminal dans le dossier « iKnowU_Server » et executer la commande suivante: 
       ```
       python ./facereco.py
       ```
       c. Selectionner la fênetre noire et appuyer la touche « c » pour changer la personne que le système est en train de chercher, et sur « q » pour arrêter le programme.
