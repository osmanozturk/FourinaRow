# FourinaRow

A simple four in a row game that created for learning purposes.
Negamax variant of the Minimax algorithm is used for implementing the AI

Scrolling mechanic implementation was forced to us in this homework, so I have tried to implement it with gridView and gridLayout but canvas felt more natural when I tried.
For implementing scrolling with canvas translated it to reverse direction of scroll. That created the illusion of a moving board. Used gestureListener for listening scroll and tap operations. Translating canvas creates the illusion of a moving board but not the scrolling board, so I am drawing a portion of background over it for creating scrolling illusion.
    
For more flexible gameplay not checked y coordinate of the tap gesture. User can make a move  anywhere from the screen with tapping within the range of a column. gesture listener waits for a complete one single tap without scrolling or another gestures to consider that as a playing operation.
Implemented all UI texts as real text for keeping translation flexibility with sacrificing the design. Also translated game to Turkish. So if the user's phone language is Turkish game will become Turkish automatically. It will display texts in English otherwise.
Main target of that game was implementing a clever AI so I have tried to implement negamax variant of the minimax AI.
Since it's complexity I couldn't implemented iterative version of it. It also not uses alpha beta pruning. So it is slow. Couldn't used it with higher depths because of that and it is not as clever as intended.
Implementing iterative version of it with alpha beta pruning is in my future plans.
I have implemented an AI difficulty selector which gives user the ability to select AI depth. Because testing with higher depths on big boards takes too long.
Because of that waiting for it to finish from main thread causes lagging and not responding game sometimes. AI calculations are done within a separate custom thread and main thread waits for it to complete before trying to make a move, informing the user about that situation with AI is thinking message. That helps for keeping consistent FPS and essential game mechanics running smoothly such as scrolling.

Osman Öztürk
osmanozturkk16@outlook.com
