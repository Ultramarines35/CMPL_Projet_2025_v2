
/*********************************************************************************
 * VARIABLES ET METHODES FOURNIES PAR LA CLASSE UtilLex (cf libClass_Projet)     *
 *       complement à l'ANALYSEUR LEXICAL produit par ANTLR                      *
 *                                                                               *
 *                                                                               *
 *   nom du programme compile, sans suffixe : String UtilLex.nomSource           *
 *   ------------------------                                                    *
 *                                                                               *
 *   attributs lexicaux (selon items figurant dans la grammaire):                *
 *   ------------------                                                          *
 *     int UtilLex.valEnt = valeur du dernier nombre entier lu (item nbentier)   *
 *     int UtilLex.numIdCourant = code du dernier identificateur lu (item ident) *
 *                                                                               *
 *                                                                               *
 *   methodes utiles :                                                           *
 *   ---------------                                                             *
 *     void UtilLex.messErr(String m)  affichage de m et arret compilation       *
 *     String UtilLex.chaineIdent(int numId) delivre l'ident de codage numId     *
 *     void afftabSymb()  affiche la table des symboles                          *
 *********************************************************************************/

import java.io.*;

/**
 * classe de mise en oeuvre du compilateur
 * =======================================
 * (verifications semantiques + production du code objet)
 * 
 * @author Girard, Masson, Perraudeau
 *
 */

public class PtGen {

	// constantes manipulees par le compilateur
	// ----------------------------------------

	private static final int

	// taille max de la table des symboles
	MAXSYMB = 300,

			// codes MAPILE :
			RESERVER = 1, EMPILER = 2, CONTENUG = 3, AFFECTERG = 4, OU = 5, ET = 6, NON = 7, INF = 8,
			INFEG = 9, SUP = 10, SUPEG = 11, EG = 12, DIFF = 13, ADD = 14, SOUS = 15, MUL = 16, DIV = 17,
			BSIFAUX = 18, BINCOND = 19, LIRENT = 20, LIREBOOL = 21, ECRENT = 22, ECRBOOL = 23,
			ARRET = 24, EMPILERADG = 25, EMPILERADL = 26, CONTENUL = 27, AFFECTERL = 28,
			APPEL = 29, RETOUR = 30,

			// codes des valeurs vrai/faux
			VRAI = 1, FAUX = 0,

			// types permis :
			ENT = 1, BOOL = 2, NEUTRE = 3,

			// categories possibles des identificateurs :
			CONSTANTE = 1, VARGLOBALE = 2, VARLOCALE = 3, PARAMFIXE = 4, PARAMMOD = 5, PROC = 6,
			DEF = 7, REF = 8, PRIVEE = 9,

			// valeurs possible du vecteur de translation
			TRANSDON = 1, TRANSCODE = 2, REFEXT = 3;

	// utilitaires de controle de type
	// -------------------------------
	/**
	 * verification du type entier de l'expression en cours de compilation
	 * (arret de la compilation sinon)
	 */
	private static void verifEnt() {
		if (tCour != ENT)
			UtilLex.messErr("expression entiere attendue");
	}

	/**
	 * verification du type booleen de l'expression en cours de compilation
	 * (arret de la compilation sinon)
	 */
	private static void verifBool() {
		if (tCour != BOOL)
			UtilLex.messErr("expression booleenne attendue");
	}

	// pile pour gerer les chaines de reprise et les branchements en avant
	// -------------------------------------------------------------------

	private static TPileRep pileRep;

	// production du code objet en memoire
	// -----------------------------------

	private static ProgObjet po;

	// COMPILATION SEPAREE
	// -------------------
	//
	/**
	 * modification du vecteur de translation associe au code produit
	 * + incrementation attribut nbTransExt du descripteur
	 * NB: effectue uniquement si c'est une reference externe ou si on compile un
	 * module
	 * 
	 * @param valeur : TRANSDON, TRANSCODE ou REFEXT
	 */
	private static void modifVecteurTrans(int valeur) {
		if (valeur == REFEXT || desc.getUnite().equals("module")) {
			po.vecteurTrans(valeur);
			desc.incrNbTansExt();
		}
	}

	// descripteur associe a un programme objet (compilation separee)
	private static Descripteur desc;

	// autres variables fournies
	// -------------------------

	// MERCI de renseigner ici un nom pour le trinome, constitue EXCLUSIVEMENT DE
	// LETTRES
	public static String trinome = "DELAPART Jules RECIPON Pierre ELOISE SINSEAU Ronald"; // TODO

	private static int tCour; // type de l'expression compilee
	private static int vCour; // sert uniquement lors de la compilation d'une valeur (entiere ou boolenne)

	// TABLE DES SYMBOLES
	// ------------------
	//
	private static EltTabSymb[] tabSymb = new EltTabSymb[MAXSYMB + 1];

	// it = indice de remplissage de tabSymb
	// bc = bloc courant (=1 si le bloc courant est le programme principal)
	private static int it, bc;

	/**
	 * utilitaire de recherche de l'ident courant (ayant pour code
	 * UtilLex.numIdCourant) dans tabSymb
	 * 
	 * @param borneInf : recherche de l'indice it vers borneInf (=1 si recherche
	 *                 dans tout tabSymb)
	 * @return : indice de l'ident courant (de code UtilLex.numIdCourant) dans
	 *         tabSymb (O si absence)
	 */
	private static int presentIdent(int borneInf) {
		int i = it;
		while (i >= borneInf && tabSymb[i].code != UtilLex.numIdCourant)
			i--;
		if (i >= borneInf)
			return i;
		else
			return 0;
	}

	/**
	 * utilitaire de placement des caracteristiques d'un nouvel ident dans tabSymb
	 * 
	 * @param code : UtilLex.numIdCourant de l'ident
	 * @param cat  : categorie de l'ident parmi CONSTANTE, VARGLOBALE, PROC, etc.
	 * @param type : ENT, BOOL ou NEUTRE
	 * @param info : valeur pour une constante, ad d'exécution pour une variable,
	 *             etc.
	 */
	private static void placeIdent(int code, int cat, int type, int info) {
		if (it == MAXSYMB)
			UtilLex.messErr("debordement de la table des symboles");
		it = it + 1;
		tabSymb[it] = new EltTabSymb(code, cat, type, info);
	}

	/**
	 * utilitaire d'affichage de la table des symboles
	 */
	private static void afftabSymb() {
		System.out.println("       code           categorie      type    info");
		System.out.println("      |--------------|--------------|-------|----");
		for (int i = 1; i <= it; i++) {
			if (i == bc) {
				System.out.print("bc=");
				Ecriture.ecrireInt(i, 3);
			} else if (i == it) {
				System.out.print("it=");
				Ecriture.ecrireInt(i, 3);
			} else
				Ecriture.ecrireInt(i, 6);
			if (tabSymb[i] == null)
				System.out.println(" reference NULL");
			else
				System.out.println(" " + tabSymb[i]);
		}
	}

	static int compteurVar;
	static int compteurVarLoc;
	static int compteurConstLoc;
	static int compteurPara;
	static int idConst;
	static int tConst;
	static int ident_tmp;
	static int affect_ident_tmp;
	static int val_tmp;
	static int tmp_boucle;
	static int appel_nb_para;
	static int nb_para_restants;
	static int placementPROC;
	/**
	 * initialisations A COMPLETER SI BESOIN
	 * -------------------------------------
	 */
	public static void initialisations() {

		// indices de gestion de la table des symboles
		it = 0;
		bc = 1;
		// pile des reprises pour compilation des branchements en avant
		pileRep = new TPileRep();
		// programme objet = code Mapile de l'unite en cours de compilation
		po = new ProgObjet();
		// COMPILATION SEPAREE: desripteur de l'unite en cours de compilation
		desc = new Descripteur();

		// initialisation necessaire aux attributs lexicaux
		UtilLex.initialisation();

		// initialisation du type de l'expression courante
		tCour = NEUTRE;
		tConst = NEUTRE;
		// TODO si necessaire
		compteurVar = 0;
		compteurPara = 0;
		compteurVarLoc = 0;
		appel_nb_para = 0;
		nb_para_restants = 0;
	} // initialisations

	/**
	 * code des points de generation A COMPLETER
	 * -----------------------------------------
	 * 
	 * @param numGen : numero du point de generation a executer
	 */
	public static void pt(int numGen) {
		switch (numGen) {
			case 0:
				initialisations();
				break;

			// TODO

			case 1: // Verifier que l element dans la pile est un booleen
				verifBool();
				break;

			case 2: // verifier que l element dans la pile est un entier
				verifEnt();
				break;

			case 3: // Modification de type : ENT

				tCour = ENT;
				break;

			case 4: // Modification de type : BOOL
				tCour = BOOL;
				break;

			case 5: // Ajout TabSymb VARGLOB/VARLOC
				if (presentIdent(bc) != 0) {
					UtilLex.messErr("Erreur : Double déclaration de variable");
				} else {
					int tmp_ident = UtilLex.numIdCourant;
					if (bc == 1) {

						placeIdent(tmp_ident, VARGLOBALE, tCour, compteurVar);
						compteurVar++;
					} else {
						placeIdent(tmp_ident, VARLOCALE, tCour, compteurVarLoc);
						compteurVarLoc++;
					}
				}
				break;

			case 6: // Actualisation Ident CONST
				// idConst = UtilLex.numIdCourant;
				break;

			case 7: // Ajout TabSymb CONST
				if (presentIdent(bc) != 0) {
					UtilLex.messErr("Erreur : Double déclaration de Constante");
				}
				else{
					placeIdent(UtilLex.numIdCourant, CONSTANTE, tCour, vCour);
					if (bc != 1) {
						compteurConstLoc++;
					}
				}
				break;

			case 8: // lecture d'une valeur entière positive ou nul
				vCour = UtilLex.valEnt;
				tCour = ENT;
				break;

			case 9: // lecture d'un entier négatif
				vCour = UtilLex.valEnt * (-1);
				tCour = ENT;
				break;

			case 10: // Reservation des Variables globales
					if (compteurVar > 0){
						po.produire(RESERVER);
						po.produire(compteurVar);
					}
					break;

			case 11: // lecture d'un ident
				ident_tmp = presentIdent(1);
				if (ident_tmp != 0) {
					//On actualise tCour
					tCour = tabSymb[ident_tmp].type;

					//on génère de quoi mettre l'ident dans la pile : contenug/contenul/empiler
					switch(tabSymb[ident_tmp].categorie){
						case VARGLOBALE:
							po.produire(CONTENUG);
							po.produire(tabSymb[ident_tmp].info);
							break;
						case VARLOCALE:
						case PARAMFIXE:
							po.produire(CONTENUL);
							po.produire(tabSymb[ident_tmp].info);
							po.produire(0);
							break;
						case PARAMMOD:
							po.produire(CONTENUL);
							po.produire(tabSymb[ident_tmp].info);
							po.produire(1);
						break;
						case CONSTANTE:
							po.produire(EMPILER);
							po.produire(tabSymb[ident_tmp].info);
							break;
						default:
							UtilLex.messErr("Erreur de la catégorie de Ident : " + tabSymb[ident_tmp].categorie);


					}
					//On modifie tCour en conséquence (on ne peut pas avoir de procédure)
					switch(tabSymb[ident_tmp].type){
						case ENT:
							tCour = ENT;
						break;
						case BOOL:
							tCour = BOOL;
						break;
						default:
							UtilLex.messErr("Erreur de type de Ident : Type neutre interdit");
						break;
					}
					ident_tmp = tabSymb[ident_tmp].info;
				} else {
					UtilLex.messErr("Erreur de type de Ident : Ident inconnu");
				}
				break;

			case 12: // Production ecrent/ecrbool (ecrire)
				if (tCour == BOOL) {
					po.produire(ECRBOOL);
				} else if (tCour == ENT) {
					po.produire(ECRENT);

				} else {
					UtilLex.messErr("Erreur de type pour ecrire : type neutre détecté");
				}
				break;

			case 13: // Empiler ident pour l'affectation
				// checker si pas constante et ils sont de meme type
				affect_ident_tmp = presentIdent(1);
				if(affect_ident_tmp == 0){
					UtilLex.messErr("Ident n'est pas dans la table");
				}
				break;

			case 14://Affectation
					//affect_ident_tmp est l'ident récupéré et est la où est stocker l'affectation
				if(tabSymb[affect_ident_tmp].type != tCour){
					UtilLex.messErr(tCour + " attendu");
				}
				else{
					//Test dans proc
					if(bc > 1){
						switch(tabSymb[affect_ident_tmp].categorie){
							case VARGLOBALE:
								po.produire(AFFECTERG);
								po.produire(tabSymb[affect_ident_tmp].info);
								break;
							case VARLOCALE:
								po.produire(AFFECTERL);
								po.produire(tabSymb[affect_ident_tmp].info);
								po.produire(0);
								break;
							case PARAMMOD:
								po.produire(AFFECTERL);
								po.produire(tabSymb[affect_ident_tmp].info);
								po.produire(1);
								break;
							case CONSTANTE:
								UtilLex.messErr("Erreur : AFFOUAPPEL, ident est une constante");
								break;
							case PARAMFIXE:
								UtilLex.messErr("Erreur : AFFOUAPPEL, ident est un paramfixe");
								break;
							default:
								UtilLex.messErr("Erreur : AFFOUAPPEL,tabSymb indique que affect_ident_tmp pointe vers def, ref ou privé");
								break;
						}
					}
					else{
						//Test hors proc
						if(tabSymb[affect_ident_tmp].categorie == VARGLOBALE){
							po.produire(AFFECTERG);
							po.produire(tabSymb[affect_ident_tmp].info);
						}
						else{
							UtilLex.messErr("Erreur : AFFOUAPPEL, ident n'est pas une varglobale");
						}
					}
				}
				break;

			case 15:
				po.produire(EG);
				tCour = BOOL;
				break;

			case 16:
				po.produire(DIFF);
				tCour = BOOL;
				break;

			case 17:
				po.produire(SUP);
				tCour = BOOL;
				break;

			case 18:
				po.produire(SUPEG);
				tCour = BOOL;
				break;

			case 19:
				po.produire(INF);
				tCour = BOOL;
				break;

			case 20:
				po.produire(INFEG);
				tCour = BOOL;
				break;

			case 22:
				po.produire(ADD);
				tCour = ENT;
				break;

			case 23:
				po.produire(SOUS);
				tCour = ENT;
				break;

			case 24:
				po.produire(MUL);
				tCour = ENT;
				break;

			case 26:
				po.produire(DIV);
				tCour = ENT;
				break;

			case 27:
				po.produire(OU);
				tCour = BOOL;
				break;

			case 28:
				po.produire(ET);
				tCour = BOOL;
				break;

			case 29:
				po.produire(NON);
				tCour = BOOL;
				break;

			case 30:// Production lirent/ Lirebool (Lire)
				ident_tmp = presentIdent(bc);
				tCour = tabSymb[ident_tmp].type;
				switch(tCour){
					case ENT:
						po.produire(LIRENT);
					break;
					case BOOL:
						po.produire(LIREBOOL);
					break;
					default:
						UtilLex.messErr("Erreur de type de Ident : Type neutre interdit");
					break;
				}
				switch(tabSymb[ident_tmp].categorie){
					case VARLOCALE:
						po.produire(AFFECTERL);
						po.produire(tabSymb[ident_tmp].info);
						po.produire(0);
						break;
					case PARAMMOD:
						po.produire(AFFECTERL);
						po.produire(tabSymb[ident_tmp].info);
						po.produire(1);
						break;
					case VARGLOBALE:
						po.produire(AFFECTERG);
						po.produire(tabSymb[ident_tmp].info);
						break;
					default:
						UtilLex.messErr("Erreur : (lire) lecture du type impossible");
						break;
				}

			case 31: // bsifaux si,ttq et cond
				po.produire(BSIFAUX);
				po.produire(0);
				pileRep.empiler(po.getIpo());
				break;

			case 32: // Résout bsifaux si et produit bincond pour le alors
				po.produire(BINCOND);
				po.produire(0);
				int ipo_bincond = po.getIpo();
				po.modifier(pileRep.depiler(), po.getIpo() + 1);
				pileRep.empiler(ipo_bincond);
				break;

			case 33: // Résout bincond du alors si y a un  sinon, sinon résout bsifaux du si
				po.modifier(pileRep.depiler(), po.getIpo() + 1);
				break;

			case 35: //Produit bincond et résout bsifaux ttq
				tmp_boucle = pileRep.depiler();
				po.produire(BINCOND);
				po.produire(pileRep.depiler());
				po.modifier(tmp_boucle, po.getIpo() + 1);
				break;

			// 36 - 39 COND
			case 36://Met le 0 en pile pour reconnaître le premier bincond
				pileRep.empiler(0);
				break;

			case 37:// On relie le nouveau BINCOND à l'adresse de l'ancien BINCOND
				po.produire(BINCOND);
				po.modifier(pileRep.depiler(), po.getIpo() + 2);
				po.produire(pileRep.depiler());
				pileRep.empiler(po.getIpo());
				break;

			case 38: // Résout le dernier bincond si pas de aut
				po.modifier(pileRep.depiler(), po.getIpo() + 1);
				break;

			case 39://Fin du cond, on résout les banchement des bincond
				int ad_temp = pileRep.depiler();
				while (ad_temp != 0) {
					int temp = po.getElt(ad_temp);
					po.modifier(ad_temp, po.getIpo() + 1);
					ad_temp = temp;
				}
				break;

			case 41:
				po.produire(EMPILER);
				po.produire(vCour);
				break;

			case 42 ://Début proc
				placeIdent(UtilLex.numIdCourant, PROC, NEUTRE, -10);
				placementPROC = it;
				placeIdent(-1, PRIVEE, NEUTRE, 0);
				bc = it + 1;
				
				compteurConstLoc = 0;
				compteurVarLoc = 0;
				compteurPara = 0;
				break;


				case 43: // decproc : parfixe
				placeIdent(UtilLex.numIdCourant, PARAMFIXE, tCour, compteurPara);
				compteurPara++;
				break;


			case 44: // decproc : parmod
				placeIdent(UtilLex.numIdCourant, PARAMMOD, tCour, compteurPara);
				compteurPara++;
				break;

			case 45: // decproc fin (Retour + Nettoyage de table)
				compteurVarLoc -= 2;
				compteurVarLoc += compteurConstLoc; //Pour enlever les constantes déclarer localement de la table
				po.produire(RETOUR);
				po.produire(compteurPara);
				for(int i = compteurVarLoc; i>0;i-- ){
					tabSymb[bc].code = -1;
					bc++;
				}
				compteurVarLoc -=compteurPara;
				it -= compteurVarLoc;
				compteurVarLoc = 0;
				bc = 1;
				break;

			case 46: // Modif proc tab Symb (Ajout des paramètres)
				tabSymb[bc - 1].info = compteurPara;
				compteurVarLoc = compteurPara + 2;
				break;

			case 47: //Reservation des varLocales
				tabSymb[placementPROC].info = po.getIpo()+1;
				int reserver_varLoc = compteurVarLoc - (compteurPara + 2);
				if(reserver_varLoc > 0){
					po.produire(RESERVER);
					po.produire(reserver_varLoc);
				}
				break;

			case 49: // Appel des procédures (effmods)
				int tmp_presentIdent = presentIdent(bc);
				if (tmp_presentIdent != 0){
					if (tabSymb[nb_para_restants].type != tabSymb[tmp_presentIdent].type  || tabSymb[nb_para_restants].categorie != PARAMMOD) {
						UtilLex.messErr("Mauvais type du  parammod en entrée de l'appel de la fonction");
					}
					switch(tabSymb[tmp_presentIdent].categorie){
						case VARGLOBALE:
							po.produire(EMPILERADG);
							po.produire(tabSymb[tmp_presentIdent].info);
							break;
						case VARLOCALE:
							po.produire(EMPILERADL);
							po.produire(tabSymb[tmp_presentIdent].info);
							po.produire(0);
							break;
						case PARAMMOD:
							po.produire(EMPILERADL);
							po.produire(tabSymb[tmp_presentIdent].info);
							po.produire(1);
							break;
						default:
							UtilLex.messErr("Erreur de type de Ident : Passage en PARAMMOD Invalide");
					}
				}
				else{
					UtilLex.messErr("Erreur de type de Ident : Passage en PARAMMOD impossible, Ident inconnu");
				}
				nb_para_restants++;
				appel_nb_para--;
				break;

			case 50:	//correction de faux
				tCour = BOOL;
				vCour = FAUX;
				break;

			case 51:	//correction de vrai
				tCour = BOOL;
				vCour = VRAI;
				break;

			case 52 : //Verifie param effixe bon type
				if (appel_nb_para > 0 ) {
					if (tabSymb[nb_para_restants].type != tCour || tabSymb[nb_para_restants].categorie != PARAMFIXE) {
						UtilLex.messErr("Mauvais type du PARAMFIXE en entrée de l'appel de la fonction");
					}
					nb_para_restants++;
					appel_nb_para--;
				} else {
					UtilLex.messErr("Trop d'élément passé en paramètres");
				}
			break;
			
			case 53 : //Verifie param effixe bon nombre
				if (appel_nb_para > 0) {
					UtilLex.messErr("Pas assez de paramètre dans effixe");
				}
				po.produire(APPEL);
				po.produire(tabSymb[affect_ident_tmp].info);
				po.produire(tabSymb[affect_ident_tmp+1].info);
			break;

			case 54 : //Setup et se place sur le premier param
				nb_para_restants = affect_ident_tmp;
				System.out.println("FEUR" + tabSymb[nb_para_restants+1].categorie);
				appel_nb_para = tabSymb[nb_para_restants+1].info;
				nb_para_restants+=2;
			break;


			case 55: // Bincond decproc
				po.produire(BINCOND);
				po.produire(0);
				pileRep.empiler(po.getIpo());
			break;
			
			case 56 : //Modification BINCOND originel (Proc) 
				if (bc == 1) { //Si nous ne sommes pas dans un PROC alors
					System.out.println("Nous avons modifiés le Bincond pour l'adresse :" + po.getIpo());
					po.modifier(4, po.getIpo() + 1);
				}
				
			break;

			case 57://Empile l'adresse avant l'expression du ttq pour le bincond
				pileRep.empiler(po.getIpo()+1);
			break;

			case 254:
				po.produire(ARRET);
				break;

			case 255:// affichage de la table des symboles en fin de compilation et production des .obj et .gen 
				afftabSymb();
				po.constObj();
				po.constGen();

				break;

			default:
				System.out.println("Point de generation non prevu dans votre liste");
				break;

		}
	}
}
