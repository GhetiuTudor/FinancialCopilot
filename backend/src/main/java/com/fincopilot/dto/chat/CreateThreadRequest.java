package com.fincopilot.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//request pt thread nou 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateThreadRequest {
    private String title;
}
