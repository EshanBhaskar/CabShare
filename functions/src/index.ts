import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();

export const deleteOldRides = functions.pubsub
    .schedule('every 24 hours')
    .onRun(async (context) => {
        const db = admin.firestore();
        const now = new Date();
        
        try {
            const ridesRef = db.collection('rides');
            const snapshot = await ridesRef
                .where('dateTime', '<', now)
                .get();
            
            const batch = db.batch();
            snapshot.docs.forEach(doc => {
                batch.delete(doc.ref);
            });
            
            await batch.commit();
            console.log(`Successfully deleted ${snapshot.size} old rides`);
            
        } catch (error) {
            console.error('Error deleting old rides:', error);
            throw error;
        }
    }); 