package com.remienzo.compassme;

public class StepSensor {

    /*
    Les buffers serviront à calculer d'abord une estimation de la vitesse instantanée à partir de plusieurs
    relevés d'accélérations, puis une vitesse moyenne depuis plusieurs estimations de vitesses instantanées.
    Le premier calcul est possible dans le cadre de pas naturels durant lesquels l'accélération du
    télépohone n'est égale à son vecteur poids qu'à l'arrêt. Le détail est dans la fontion update().
     */
    private static final int SPEED_BUFFER_SIZE = 10;
    private static final int ACC_BUFFER_SIZE = SPEED_BUFFER_SIZE * 5;

    /*
    Vitesse au-delà de laquelle on considère que le téléphone est embarqué dans un pas.
    Cette valeur est complètement empirique et dépend peut-être bien de l'appareil.
     */
    private static final float BE_THIS_FAST_TO_BE_A_STEP = 0.2f;

    private int bufferedValues = 0; // Nombre de mesures effectuées (pour faire des moyennes propres)
    private float[] accBufferX = new float[ACC_BUFFER_SIZE];
    private float[] accBufferY = new float[ACC_BUFFER_SIZE]; // Buffer de valeurs d'accélération, en 3D pour comparer avec le vecteur poids
    private float[] accBufferZ = new float[ACC_BUFFER_SIZE];
    private float[] speedBuffer = new float[SPEED_BUFFER_SIZE]; // Buffer vitesse, ici seule la norme nous intéresse (d'où non 3D)
    private boolean aStepJustOccured = false; // Permet de savoir si l'onvient de faire un pas pour ne pas le recompter
    private float previousSpeed = 0; // Vitesse moyenne du téléphone à la mesure précédente

    private StepListener listener;

    public void setListener(StepListener listener) {
        this.listener = listener;
    }

    /*
    D'abord quelques fonctions très classiques dont nous aurons besoin pour manipuler des vecteurs
     */

    public float sum(float[] array) {
        float retval = 0;
        for (int i = 0; i < array.length; i++) {
            retval += array[i];
        }
        return retval;
    }

    public float[] cross(float[] arrayA, float[] arrayB) {
        float[] retArray = new float[3];
        retArray[0] = arrayA[1] * arrayB[2] - arrayA[2] * arrayB[1];
        retArray[1] = arrayA[2] * arrayB[0] - arrayA[0] * arrayB[2];
        retArray[2] = arrayA[0] * arrayB[1] - arrayA[1] * arrayB[0];
        return retArray;
    }

    public float norm(float[] array) {
        float retval = 0;
        for (int i = 0; i < array.length; i++) {
            retval += array[i] * array[i];
        }
        return (float) Math.sqrt(retval);
    }


    public float dot(float[] a, float[] b) {
        float retval = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return retval;
    }

    public float[] normalize(float[] a) {
        float[] retval = new float[a.length];
        float norm = norm(a);
        for (int i = 0; i < a.length; i++) {
            retval[i] = a[i] / norm;
        }
        return retval;
    }

    /*
    Le but est maintenant de trouver la vitesse du téléphone, indifféremment de son orientation.
     */

    public void update(float acc_x, float acc_y, float acc_z) {
        // On récupère les valeurs lancées par l'accéléromètre dans un vecteur accélération
        float[] currentAcc = new float[3];
        currentAcc[0] = acc_x;
        currentAcc[1] = acc_y;
        currentAcc[2] = acc_z;

        // Remplissage du buffer d'accélérations. L'ordre importe peu pour notre méthode à base de moyennes.
        bufferedValues++;
        accBufferX[bufferedValues % ACC_BUFFER_SIZE] = currentAcc[0];
        accBufferY[bufferedValues % ACC_BUFFER_SIZE] = currentAcc[1];
        accBufferZ[bufferedValues % ACC_BUFFER_SIZE] = currentAcc[2];

        /*
        On estime ici le vecteur poids (ou z pour les intimes) à l'aide des mesures d'accélérations dans le buffer.
        Le principe est le suivant : dans un monde parfait, l'accélération du téléphone en fonction du temps est
        une fonction périodique, qui vérifie une propriété intéressante. Lors d'un pas, le téléphone pars d'une position à une vitesse
        V et se retrouve plus loin, mais à la même vitesse V. Il a donc autant accéléré qu'il a décéléré.
        En d'autres termes, la moyenne des relevés de l'accélération, s'ils sont échantillonés sur une durée
        supérieure à la périodicité des pas, est exactement le vecteur poids.
        C'est tout de même sympa les maths.
         */
        float[] zVector = new float[3];
        zVector[0] = sum(accBufferX) / Math.min(bufferedValues, ACC_BUFFER_SIZE);
        zVector[1] = sum(accBufferY) / Math.min(bufferedValues, ACC_BUFFER_SIZE);
        zVector[2] = sum(accBufferZ) / Math.min(bufferedValues, ACC_BUFFER_SIZE);


        /*
        On calcule ici la "véritable" accélération, dépourvue du poids. Et puisque l'on s'intéresse
        surtout au mouvement horizontal, un produit vectoriel permet de ne pas compter les pas
        de quelqu'un qui piétine sur place.
        */
        float trueAcc = norm(cross(zVector, currentAcc));
        /*
        Enfin, on traite ces accélérations pures comme s'il s'agissait de vitesses instantanées.
        Ainsi, chaque pas est compté deux fois (à l'accélération de la jambe puis à sa décélération)
        pour compter les deux jambes. C'est bien pratique.
         */
        speedBuffer[bufferedValues % SPEED_BUFFER_SIZE] = trueAcc;

        float meanSpeed = sum(speedBuffer) / Math.min(bufferedValues, ACC_BUFFER_SIZE);

        /*
        Ne reste plus qu'à savoir si l'on vient de faire un pas, et si ce pas n'a pas déjà été compté.
         */
        if ((meanSpeed - BE_THIS_FAST_TO_BE_A_STEP) * (previousSpeed - BE_THIS_FAST_TO_BE_A_STEP) < 0) {
            if(!aStepJustOccured) {
                listener.stepOccurred();
                aStepJustOccured = true;
            }
        }
        else {
            aStepJustOccured = false;
        }
        previousSpeed = meanSpeed;
    }
}
