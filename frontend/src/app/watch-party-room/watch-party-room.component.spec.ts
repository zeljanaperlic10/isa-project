import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WatchPartyRoomComponent } from './watch-party-room.component';

describe('WatchPartyRoomComponent', () => {
  let component: WatchPartyRoomComponent;
  let fixture: ComponentFixture<WatchPartyRoomComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [WatchPartyRoomComponent]
    });
    fixture = TestBed.createComponent(WatchPartyRoomComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
