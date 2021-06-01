

void setup() {
  Serial.begin(115200);
}

int  count = 0;

void loop() {
 
  char received[3];
  int i = 0;

  while (Serial.available() > 0)
  {
    received[i] = Serial.read();
    i++;
  }

  if (i != 0)
  {   
      Serial.print(received[0]);
      Serial.print(" .. ");
      Serial.println(count);
  } 
  else
  {
    count++;
  }
}
