package com.survisha.meghaconnect.face.service;

import com.survisha.meghaconnect.face.dto.FaceRequests;
import com.survisha.meghaconnect.face.dto.FaceResponses;

public interface FaceRecognitionService {
    FaceResponses.Enroll enroll(FaceRequests.Enroll request);
    FaceResponses.Compare compare(FaceRequests.Compare request);
    FaceResponses.Delete delete(FaceRequests.Delete request);
    FaceResponses.Search search(FaceRequests.Search request, boolean mayReturnMatchedPhoto);
    FaceResponses.Verify verify(FaceRequests.Verify request);
}
